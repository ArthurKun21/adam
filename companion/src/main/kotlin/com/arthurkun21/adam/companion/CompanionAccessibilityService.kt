/*
 * Copyright 2026 Google LLC
 * Portions Copyright (C) 2026 ArthurKun21
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported to Kotlin from google/artemis packages/artemis-accessibility-helper (Apache 2.0)

package com.arthurkun21.adam.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Non-exclusive accessibility service with foreground keep-alive support.
 *
 * Runs concurrently with any Android UI testing framework (Mobly, Appium, Espresso)
 * without monopolizing Android's singleton UiAutomationService connection.
 *
 * The service subscribes to window-state changes only: that is the single event it
 * uses (to remember the foreground package / activity), and anything wider costs
 * CPU and battery on a device that is idle between tasks.
 */
class CompanionAccessibilityService : AccessibilityService() {

    /** Bumped whenever the HTTP contract changes in a way an older host cannot use. */
    var currentPackageName: String = ""
        private set
    var currentActivityName: String = ""
        private set

    private var server: CompanionCommandServer? = null

    /** Installed helper versionCode, or -1 when the package manager cannot answer. */
    @Suppress("DEPRECATION")
    val versionCode: Long
        get() = try {
            val info = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
        } catch (t: Throwable) {
            -1L
        }

    val versionName: String
        get() = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (t: Throwable) {
            ""
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "CompanionAccessibilityService connected")

        // 1. Start foreground service to resist low-memory killer on aggressive custom ROMs
        startForegroundNotification()

        // 2. Dynamically enforce flags to guarantee compatibility across custom OEM ROMs
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 100
            info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            // Not-important views are what UIAutomator's compressed dump leaves out;
            // keep them out here too so both backends describe the same tree.
            info.flags = info.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
            serviceInfo = info
            Log.i(TAG, "AccessibilityServiceInfo flags dynamically enforced")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to dynamically configure AccessibilityServiceInfo", t)
        }

        // 3. Start local loopback command server
        server?.shutdown()
        server = CompanionCommandServer(this, DEFAULT_PORT).also {
            it.setDaemon(true)
            it.start()
        }
        Log.i(TAG, "CompanionCommandServer started on port $DEFAULT_PORT")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    @Suppress("DEPRECATION")
    private fun startForegroundNotification() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
                } catch (ignored: Throwable) {
                }
                // Ask for IMPORTANCE_MIN (no sound, collapsed in the shade). Android raises
                // foreground-service channels to LOW at most, which is still silent. The
                // notification exists only to keep the service alive on aggressive ROMs;
                // it must not look like something the person needs to act on.
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "adam test helper",
                    NotificationManager.IMPORTANCE_MIN,
                )
                channel.description = "Shown while the adam test helper is installed on this device"
                channel.setShowBadge(false)
                nm.createNotificationChannel(channel)
            }

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                Notification.Builder(this)
            }

            builder.setContentTitle("adam test helper is running")
                .setContentText(
                    "Lets adam read this screen during automated tests. " +
                        "Remove it any time from Settings > Apps.",
                )
                .setStyle(
                    Notification.BigTextStyle().bigText(
                        "Installed by adam to read the screen layout during automated " +
                            "tests. It answers only on this device (127.0.0.1:$DEFAULT_PORT" +
                            "), only to the computer that is connected over ADB and holds " +
                            "the session token, and it sends nothing anywhere. Remove it " +
                            "any time from Settings > Apps.",
                    ),
                )
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setPriority(Notification.PRIORITY_MIN)
            }

            when {
                Build.VERSION.SDK_INT >= 34 ->
                    startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    startForeground(NOTIFICATION_ID, builder.build(), 0)

                else -> startForeground(NOTIFICATION_ID, builder.build())
            }
            Log.i(TAG, "Foreground keep-alive notification active")
        } catch (t: Throwable) {
            Log.w(TAG, "Foreground notification start skipped or deferred: ${t.message}")
        }
    }

    private fun stopForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (ignored: Throwable) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let { currentPackageName = it.toString() }
            event.className?.let { currentActivityName = it.toString() }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "CompanionAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "CompanionAccessibilityService destroyed")
        stopForegroundNotification()
        server?.shutdown()
        server = null
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "AdamCompanionA11y"
        const val DEFAULT_PORT = 18888

        /**
         * 2: token authentication, byte-accurate bodies, visible-only dumps, fields=
         */
        const val PROTOCOL_VERSION = 2

        // Channel importance is frozen by Android once a channel exists, so a quieter
        // channel needs a new id; the pre-1.1.3 channel is deleted on start.
        private const val LEGACY_CHANNEL_ID = "adam_companion_channel"
        private const val CHANNEL_ID = "adam_companion_quiet"
        private const val NOTIFICATION_ID = 18888

        @Volatile
        var instance: CompanionAccessibilityService? = null
            private set
    }
}
