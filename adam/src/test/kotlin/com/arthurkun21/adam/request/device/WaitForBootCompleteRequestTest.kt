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

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeoutException

class WaitForBootCompleteRequestTest {
    @get:Rule
    val timeoutRule = Timeout(30_000)

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsWhenBootCompleted() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "getprop sys.boot_completed;echo x$?" }
                            .accept()
                            .respond("1\nx0")
                    }
                }
            }

            client.execute(
                WaitForBootCompleteRequest(timeoutMs = 5_000, pollIntervalMs = 100),
                serial = "serial",
            )
        }
    }

    @Test
    fun testTimesOutWhenBootNeverCompletes() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    // each poll opens a new connection, register enough scripts to cover the poll window
                    repeat(10) {
                        session {
                            respondOkay()
                            expectShell { "getprop sys.boot_completed;echo x$?" }
                                .accept()
                                .respond("0\nx0")
                        }
                    }
                }
            }

            try {
                client.execute(
                    WaitForBootCompleteRequest(timeoutMs = 500, pollIntervalMs = 100),
                    serial = "serial",
                )
                throw AssertionError("Expected TimeoutException")
            } catch (expected: TimeoutException) {
            }
        }
    }
}
