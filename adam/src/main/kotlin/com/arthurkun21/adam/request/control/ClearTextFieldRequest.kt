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

import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Clears the currently focused text field: moves the cursor to the end, selects everything with a
 * shift+home, deletes the selection and falls back to a burst of backspaces for the remainder
 */
public class ClearTextFieldRequest : ShellCommandRequest(cmd = CLEAR_TEXT_COMMAND) {
    public companion object {
        private const val KEYCODE_MOVE_END = 123
        private const val KEYCODE_MOVE_HOME = 122
        private const val KEYCODE_DEL = 67
        private const val META_SHIFT = 1
        private const val FALLBACK_BACKSPACES = 20

        private val CLEAR_TEXT_COMMAND = buildString {
            append("input keyevent $KEYCODE_MOVE_END && ")
            append("input keyevent --meta $META_SHIFT $KEYCODE_MOVE_HOME && ")
            append("input keyevent $KEYCODE_DEL && ")
            append("input keyevent")
            repeat(FALLBACK_BACKSPACES) { append(" $KEYCODE_DEL") }
        }
    }
}
