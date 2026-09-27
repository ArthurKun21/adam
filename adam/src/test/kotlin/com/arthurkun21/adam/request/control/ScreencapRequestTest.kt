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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ScreencapRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        val bytes = ScreencapRequest().serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0011exec:screencap -p")
    }

    @Test
    fun testReturnsRawContent() {
        runBlocking {
            val pngBytes = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x01, 0x02, 0x03, 0x7F, 0x40.toByte(), 0x7E, 0x15.toByte(),
            )
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "exec:screencap -p" }.accept()
                output.respond(pngBytes)
            }

            val output = client.execute(ScreencapRequest(), serial = "serial")
            assertThat(output).isEqualTo(pngBytes)
        }
    }
}
