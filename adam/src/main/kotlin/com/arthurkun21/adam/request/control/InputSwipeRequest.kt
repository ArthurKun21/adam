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
 * Swipes from the start coordinates to the end coordinates
 *
 * @param durationMs swipe duration in milliseconds. A longer duration turns the swipe into a drag
 */
public class InputSwipeRequest(
    private val startX: Int,
    private val startY: Int,
    private val endX: Int,
    private val endY: Int,
    private val durationMs: Long = DEFAULT_DURATION_MS,
) : ShellCommandRequest(
    cmd = "input swipe $startX $startY $endX $endY $durationMs",
) {
    override fun validate(): ValidationResponse = when {
        startX < 0 || startY < 0 || endX < 0 || endY < 0 -> {
            ValidationResponse(false, "coordinates must be non-negative")
        }

        durationMs <= 0 -> ValidationResponse(false, "durationMs must be positive")

        else -> ValidationResponse.Success
    }

    public companion object {
        public const val DEFAULT_DURATION_MS: Long = 800L

        /**
         * Long-press expressed as a same-point swipe
         */
        public fun longPress(
            x: Int,
            y: Int,
            durationMs: Long = 1000L,
        ): InputSwipeRequest = InputSwipeRequest(x, y, x, y, durationMs)
    }
}
