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
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager

/**
 * Universal display and orientation utilities compatible across all Android versions.
 */
object DisplayUtils {

    data class DisplayInfo(val rotation: Int, val width: Int, val height: Int)

    /**
     * Obtains the display orientation (0, 1, 2, 3) and screen dimensions (width, height)
     * using the most reliable API for the current Android runtime.
     */
    @Suppress("DEPRECATION")
    fun getDisplayInfo(service: AccessibilityService): DisplayInfo {
        var rotation = 0
        var width = 1080
        var height = 2400

        // Baseline initialization from resources metrics (handles tablets, TVs, emulators dynamically)
        try {
            val resDm: DisplayMetrics = service.resources.displayMetrics
            if (resDm.widthPixels > 0 && resDm.heightPixels > 0) {
                width = resDm.widthPixels
                height = resDm.heightPixels
            }
        } catch (ignored: Throwable) {
        }

        try {
            // Modern API 30+ window metrics for full physical screen dimensions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val wm = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    if (wm != null) {
                        val bounds: Rect = wm.currentWindowMetrics.bounds
                        if (bounds.width() > 0 && bounds.height() > 0) {
                            width = bounds.width()
                            height = bounds.height()
                        }
                    }
                } catch (ignored: Throwable) {
                }
            }

            // Resolve Display instance for rotation and legacy metrics
            var display: Display? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    display = service.display
                } catch (ignored: Throwable) {
                }
            }

            if (display == null) {
                val wm = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                display = wm?.defaultDisplay
            }

            if (display != null) {
                rotation = when (display.rotation) {
                    Surface.ROTATION_90 -> 1
                    Surface.ROTATION_180 -> 2
                    Surface.ROTATION_270 -> 3
                    else -> 0
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    val dm = DisplayMetrics()
                    display.getRealMetrics(dm)
                    width = dm.widthPixels
                    height = dm.heightPixels
                }
            }
        } catch (ignored: Throwable) {
        }

        return DisplayInfo(rotation, width, height)
    }
}
