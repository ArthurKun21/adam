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

package com.malinskiy.adam.request.framebuffer

public enum class ColorSpace {
    UNKNOWN,
    SRGB,
    P3,
    ;

    public fun getProfileName(): String? = when (this) {
        UNKNOWN -> null
        SRGB -> "sRGB.icc"
        P3 -> "DisplayP3.icc"
    }

    public companion object {
        public fun from(value: Int): ColorSpace = when (value) {
            1 -> SRGB
            2 -> P3
            else -> UNKNOWN
        }
    }
}
