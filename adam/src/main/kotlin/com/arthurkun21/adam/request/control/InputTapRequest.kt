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

package com.arthurkun21.adam.request.control

import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Taps the screen at the given coordinates
 *
 * @param x x coordinate in pixels
 * @param y y coordinate in pixels
 * @param durationMs when provided, the tap is expressed as a same-point swipe with the given
 * duration (press-and-hold). A duration of 500ms or more reliably produces a long-press on the
 * device, shorter values behave like a regular tap
 */
public class InputTapRequest(
    private val x: Int,
    private val y: Int,
    private val durationMs: Long? = null,
) : ShellCommandRequest(
    cmd = when (durationMs) {
        null -> "input tap $x $y"
        else -> "input swipe $x $y $x $y $durationMs"
    },
) {
    override fun validate(): ValidationResponse = when {
        x < 0 || y < 0 -> ValidationResponse(false, "x and y must be non-negative")
        durationMs != null && durationMs <= 0 -> ValidationResponse(false, "durationMs must be positive")
        else -> ValidationResponse.Success
    }
}
