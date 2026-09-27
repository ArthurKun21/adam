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

package com.arthurkun21.adam.request.settings

import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Sends a broadcast via `am broadcast`
 *
 * Used for token delivery to exported receivers reachable by the adb shell user (e.g. the
 * companion service token receiver) and for IME text injection via ADBKeyboard's `ADB_INPUT_B64`
 * action
 *
 * @param action intent action, e.g. `com.arthurkun21.adam.companion.SET_TOKEN`
 * @param component target component in the `pkg/.Receiver` form, e.g.
 * `com.arthurkun21.adam.companion/.TokenReceiver`
 * @param stringExtras string extras delivered with the broadcast
 */
public class AmBroadcastRequest(
    private val action: String? = null,
    private val component: String? = null,
    private val stringExtras: Map<String, String> = emptyMap(),
) : ShellCommandRequest(
    cmd = StringBuilder("am broadcast").apply {
        action?.let { append(" -a $it") }
        component?.let { append(" -n $it") }
        stringExtras.forEach { (key, value) -> append(" --es '$key' '$value'") }
    }.toString(),
) {
    override fun validate(): ValidationResponse = when {
        action.isNullOrBlank() && component.isNullOrBlank() -> {
            ValidationResponse(false, "Either action or component must be provided")
        }

        else -> ValidationResponse.Success
    }
}
