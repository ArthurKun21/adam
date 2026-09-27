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
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeoutException

class WaitForDeviceRequestTest {
    @get:Rule
    val timeoutRule = Timeout(30_000)

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsWhenDeviceMatchesState() {
        runBlocking {
            server.server.multipleSessions {
                other("host:devices") {
                    respondOkay()
                    respondListDevices(mapOf("emulator-5554" to "device"))
                }
            }

            val output = client.execute(
                WaitForDeviceRequest("emulator-5554", DeviceState.DEVICE, timeoutMs = 5_000, pollIntervalMs = 100),
            )
            assertThat(output).isEqualTo(Device("emulator-5554", DeviceState.DEVICE))
        }
    }

    @Test
    fun testWaitsUntilStateMatches() {
        runBlocking {
            server.server.multipleSessions {
                other("host:devices") {
                    respondOkay()
                    respondListDevices(mapOf("emulator-5554" to "unauthorized"))
                }
            }

            try {
                client.execute(
                    WaitForDeviceRequest("emulator-5554", DeviceState.DEVICE, timeoutMs = 500, pollIntervalMs = 100),
                )
                throw AssertionError("Expected TimeoutException")
            } catch (expected: TimeoutException) {
                assertThat(expected.message).isEqualTo("Device emulator-5554 did not reach state DEVICE within 500ms")
            }
        }
    }
}
