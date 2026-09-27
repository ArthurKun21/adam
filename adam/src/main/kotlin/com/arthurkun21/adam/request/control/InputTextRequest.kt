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
 * Types text into the currently focused input field
 *
 * Lines are sent sequentially with an ENTER key event in between since `input text` cannot type
 * newlines. Non-ASCII input is not reliably supported by `input text`: for full Unicode support,
 * deploy ADBKeyboard on the device and use [com.arthurkun21.adam.request.settings.AmBroadcastRequest]
 * with the `ADB_INPUT_B64` action
 */
public class InputTextRequest(private val text: String) : ShellCommandRequest(cmd = buildInputTextCommand(text)) {
    override fun validate(): ValidationResponse = when {
        text.isEmpty() -> ValidationResponse(false, "text must not be empty")
        else -> ValidationResponse.Success
    }

    public companion object {
        /**
         * Escapes special characters for `adb shell input text`.
         * Shell metacharacters are backslash-escaped and spaces are encoded as `%s`
         */
        public fun escapeText(value: String): String =
            value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("&", "\\&")
                .replace("|", "\\|")
                .replace(";", "\\;")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace("~", "\\~")
                .replace(" ", "%s")

        public fun buildInputTextCommand(text: String): String {
            val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
            val parts = mutableListOf<String>()
            normalized.split("\n").forEachIndexed { index, line ->
                if (index > 0) parts += "input keyevent ${AndroidKeyCode.ENTER.code}"
                if (line.isNotEmpty()) parts += "input text ${escapeText(line)}"
            }
            return parts.joinToString(" && ")
        }
    }
}
