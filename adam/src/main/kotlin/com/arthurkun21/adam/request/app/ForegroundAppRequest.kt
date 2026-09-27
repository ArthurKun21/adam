/*
 * Copyright (C) 2021 Anton Malinskiy
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

package com.arthurkun21.adam.request.app

import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.shell.v1.SyncShellCommandRequest

/**
 * Detects the foreground application via `dumpsys window`
 *
 * @return the foreground package or null if it could not be determined (e.g. the launcher screen
 * on some Android versions or a `dumpsys window displays` subcommand that is not supported)
 */
public class ForegroundAppRequest : SyncShellCommandRequest<ForegroundApp?>(
    cmd = "dumpsys window displays | grep -E 'mCurrentFocus|mFocusedApp'",
) {
    override fun convertResult(response: ShellCommandResult): ForegroundApp? {
        val lines = response.output.lines()

        // A focused window that explicitly names package/activity is the most precise signal
        for (line in lines) {
            if (!line.contains("mCurrentFocus")) continue
            val match = WINDOW_PATTERN.find(line) ?: continue
            val activity = match.groupValues[2].takeIf { it.isNotEmpty() }
            if (activity != null) {
                return ForegroundApp(match.groupValues[1], activity)
            }
        }

        // System windows (notification shade, lockscreen) have no package: fall back to the
        // focused application
        for (line in lines) {
            if (!line.contains("mFocusedApp")) continue
            val match = WINDOW_PATTERN.find(line) ?: continue
            return ForegroundApp(match.groupValues[1], match.groupValues[2].takeIf { it.isNotEmpty() })
        }

        // Last resort: a focused system window without package info
        for (line in lines) {
            val match = WINDOW_PATTERN.find(line) ?: continue
            return ForegroundApp(match.groupValues[1], null)
        }
        return null
    }

    public companion object {
        /**
         * Matches `mCurrentFocus=Window{abc123 u0 com.example.app/com.example.app.MainActivity}`
         * and `mFocusedApp=ActivityRecord{abc123 u0 com.example.app/.MainActivity t42}`
         */
        private val WINDOW_PATTERN = Regex("""(?:Window|ActivityRecord)\{\S+ (?:u\d+ )?([^/\s}]+)(?:/([^\s}]+))?""")
    }
}

/**
 * @param packageName foreground package name
 * @param activity activity component if resolvable
 */
public data class ForegroundApp(
    public val packageName: String,
    public val activity: String? = null,
)
