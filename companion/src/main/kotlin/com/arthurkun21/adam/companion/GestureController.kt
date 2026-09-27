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
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Synchronous gesture and action executor using native AccessibilityService APIs.
 * Works without requiring UiAutomation or root privileges.
 */
object GestureController {

    private const val TAG = "AdamGestureCtrl"
    private val MAIN_HANDLER = Handler(Looper.getMainLooper())

    fun tap(service: AccessibilityService, x: Float, y: Float, timeoutMs: Long): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchSynchronous(service, gesture, timeoutMs)
    }

    fun doubleTap(service: AccessibilityService, x: Float, y: Float, timeoutMs: Long): Boolean {
        if (!tap(service, x, y, timeoutMs)) return false
        try {
            Thread.sleep(100L)
        } catch (ignored: InterruptedException) {
        }
        return tap(service, x, y, timeoutMs)
    }

    fun longPress(
        service: AccessibilityService,
        x: Float,
        y: Float,
        durationMs: Long,
        timeoutMs: Long,
    ): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val pressDur = durationMs.coerceIn(500L, 5000L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, pressDur)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchSynchronous(service, gesture, timeoutMs + pressDur)
    }

    fun swipe(
        service: AccessibilityService,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long,
        timeoutMs: Long,
    ): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val safeDuration = durationMs.coerceIn(50L, 5000L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchSynchronous(service, gesture, timeoutMs + safeDuration)
    }

    /**
     * Sets (or, with `append`, extends) the text of the focused / first editable field.
     * ACTION_SET_TEXT replaces the whole content; appending mirrors what typing through an
     * IME does, which is the contract the host's send_text has always had.
     */
    fun setText(service: AccessibilityService, text: String?, append: Boolean): Boolean {
        val inputNode = HierarchyDumper.findInputNode(service) ?: run {
            Log.w(TAG, "No editable/focused input node found for setText")
            return false
        }
        try {
            var value = text ?: ""
            if (append) {
                val existing = inputNode.text
                var showingHint = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        showingHint = inputNode.isShowingHintText
                    } catch (ignored: Throwable) {
                    }
                }
                if (!existing.isNullOrEmpty() && !showingHint) {
                    value = existing.toString() + value
                }
            }
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            return inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (t: Throwable) {
            Log.w(TAG, "performAction ACTION_SET_TEXT failed", t)
            return false
        } finally {
            HierarchyDumper.safeRecycle(inputNode)
        }
    }

    fun clearText(service: AccessibilityService): Boolean = setText(service, "", false)

    fun performGlobalAction(service: AccessibilityService, actionName: String?): Boolean {
        if (actionName == null) return false
        val actionId = when (actionName.lowercase()) {
            "back" -> AccessibilityService.GLOBAL_ACTION_BACK

            "home" -> AccessibilityService.GLOBAL_ACTION_HOME

            "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS

            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

            "quick_settings" -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

            "power_dialog" -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG

            "toggle_split_screen" -> AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN

            "lock_screen" ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    return false
                }

            "take_screenshot" ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                } else {
                    return false
                }

            else -> return false
        }
        return service.performGlobalAction(actionId)
    }

    private fun dispatchSynchronous(
        service: AccessibilityService,
        gesture: GestureDescription,
        timeoutMs: Long,
    ): Boolean {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)

        MAIN_HANDLER.post {
            try {
                service.dispatchGesture(
                    gesture,
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            result.set(true)
                            latch.countDown()
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            result.set(false)
                            latch.countDown()
                        }
                    },
                    null,
                )
            } catch (t: Throwable) {
                Log.w(TAG, "dispatchGesture threw exception", t)
                result.set(false)
                latch.countDown()
            }
        }

        return try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS) && result.get()
        } catch (e: InterruptedException) {
            false
        }
    }

    /**
     * Write text to the system clipboard. Clipboard writes are allowed from any
     * app on every API level (only reads are restricted since Android 10), so this
     * is the IME-free path the host uses for multiline and non-ASCII input:
     * set the clip here, then KEYCODE_PASTE over adb.
     */
    fun setClipboard(service: AccessibilityService, text: String?): Boolean {
        val latch = CountDownLatch(1)
        val ok = AtomicBoolean(false)
        MAIN_HANDLER.post {
            try {
                val cm = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("adam", text ?: ""))
                    ok.set(true)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "setClipboard failed: ${t.message}")
            } finally {
                latch.countDown()
            }
        }
        try {
            latch.await(2000, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return ok.get()
    }
}
