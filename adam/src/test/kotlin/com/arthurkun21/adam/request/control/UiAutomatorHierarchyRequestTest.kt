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
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class UiAutomatorHierarchyRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsHierarchy() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "uiautomator dump --compressed /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("UI hierchary dumped to: /data/local/tmp/adam_window_dump.xml\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell { "cat /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("<hierarchy rotation=\"0\">\n</hierarchy>\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell { "rm -f /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                }
            }

            val output = client.execute(UiAutomatorHierarchyRequest(), serial = "serial")
            assertThat(output).isEqualTo("<hierarchy rotation=\"0\">\n</hierarchy>")
        }
    }

    @Test
    fun testStripsNullCharactersFromUtf16Dumps() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "uiautomator dump --compressed /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("UI hierchary dumped to: /data/local/tmp/adam_window_dump.xml\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell { "cat /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("<hierarchy>\u0000rotation=\"0\"\u0000</hierarchy>x0")
                    }
                    session {
                        respondOkay()
                        expectShell { "rm -f /data/local/tmp/adam_window_dump.xml;echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                }
            }

            val output = client.execute(UiAutomatorHierarchyRequest(), serial = "serial")
            assertThat(output).isEqualTo("<hierarchy>rotation=\"0\"</hierarchy>")
        }
    }
}
