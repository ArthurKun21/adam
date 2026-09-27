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

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Dumps the active window hierarchy with the on-device `uiautomator dump` command and returns it
 * as XML in the standard uiautomator format (`<hierarchy rotation="...">`)
 *
 * This is a pure-ADB fallback for hierarchy inspection. On devices with the companion
 * accessibility service deployed, prefer [com.arthurkun21.adam.companion.CompanionClient] which
 * produces the same format with real-time accessibility data
 *
 * Some older devices write the dump in UTF-16; the raw output is normalized by stripping null
 * characters before decoding
 *
 * @param remotePath on-device location of the temporary dump file
 */
public class UiAutomatorHierarchyRequest(
    private val remotePath: String = DEFAULT_REMOTE_PATH,
    private val compressed: Boolean = true,
) : MultiRequest<String>() {
    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?,
    ): String = with(androidDebugBridgeClient) {
        val flags = when (compressed) {
            true -> "--compressed "
            false -> ""
        }
        execute(ShellCommandRequest("uiautomator dump ${flags}$remotePath"), serial)
        val dump = execute(ShellCommandRequest("cat $remotePath"), serial)
        execute(ShellCommandRequest("rm -f $remotePath"), serial)
        decode(dump.output)
    }

    private fun decode(raw: String): String = raw
        .replace("\u0000", "")
        .replace("\uFEFF", "")
        .trim()

    public companion object {
        public const val DEFAULT_REMOTE_PATH: String = "/data/local/tmp/adam_window_dump.xml"
    }
}
