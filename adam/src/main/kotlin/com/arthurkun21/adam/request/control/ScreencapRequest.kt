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

import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxPacketBuffer
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream

/**
 * Captures the device screen as a PNG image via the raw `exec` service (`screencap -p`).
 *
 * The `exec` service is the raw variant of `shell` (no pty), so binary output is not mangled —
 * this is the same wire-level service `adb exec-out` uses. For raw framebuffer frames use
 * [com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest] instead
 */
public class ScreencapRequest : ComplexRequest<ByteArray>() {
    override fun serialize(): ByteArray = createBaseRequest("exec:screencap -p")

    override suspend fun readElement(socket: Socket): ByteArray {
        val output = ByteArrayOutputStream()
        withMaxPacketBuffer {
            val buffer = array()
            loop@ while (true) {
                val available = socket.readAvailable(buffer, 0, buffer.size)
                when {
                    available < 0 -> break@loop

                    available > 0 -> {
                        output.write(buffer, 0, available)
                        yield()
                    }

                    else -> yield()
                }
            }
        }
        return output.toByteArray()
    }
}
