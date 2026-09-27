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

package com.arthurkun21.adam.request.device

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class GetStateRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        val bytes = GetStateRequest("serial").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("001Chost-serial:serial:get-state")
    }

    @Test
    fun testReturnsProperState() {
        runBlocking {
            server.session {
                expectCmd { "host-serial:serial:get-state" }.accept()
                output.respondStringV1("device")
            }

            val output = client.execute(GetStateRequest("serial"))
            assertThat(output).isEqualTo(DeviceState.DEVICE)
        }
    }

    @Test
    fun testReturnsOfflineState() {
        runBlocking {
            server.session {
                expectCmd { "host-serial:serial:get-state" }.accept()
                output.respondStringV1("offline")
            }

            val output = client.execute(GetStateRequest("serial"))
            assertThat(output).isEqualTo(DeviceState.OFFLINE)
        }
    }
}
