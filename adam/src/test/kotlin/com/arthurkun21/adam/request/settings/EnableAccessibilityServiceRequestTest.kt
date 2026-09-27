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

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EnableAccessibilityServiceRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testEnablesServiceWhenUnset() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "settings get secure enabled_accessibility_services;echo x$?" }
                            .accept()
                            .respond("null\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell { "settings put secure 'enabled_accessibility_services' 'com.pkg/.Svc';echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                    session {
                        respondOkay()
                        expectShell { "settings put secure 'accessibility_enabled' '1';echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                }
            }

            client.execute(EnableAccessibilityServiceRequest("com.pkg/.Svc"), serial = "serial")
        }
    }

    @Test
    fun testAppendsToExistingServices() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "settings get secure enabled_accessibility_services;echo x$?" }
                            .accept()
                            .respond("com.a/.OtherSvc\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell {
                            "settings put secure 'enabled_accessibility_services' 'com.a/.OtherSvc:com.pkg/.Svc';echo x$?"
                        }
                            .accept()
                            .respond("x0")
                    }
                    session {
                        respondOkay()
                        expectShell { "settings put secure 'accessibility_enabled' '1';echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                }
            }

            client.execute(EnableAccessibilityServiceRequest("com.pkg/.Svc"), serial = "serial")
        }
    }

    @Test
    fun testSkipsPutWhenAlreadyEnabled() {
        runBlocking {
            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShell { "settings get secure enabled_accessibility_services;echo x$?" }
                            .accept()
                            .respond("com.pkg/.Svc\nx0")
                    }
                    session {
                        respondOkay()
                        expectShell { "settings put secure 'accessibility_enabled' '1';echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                }
            }

            client.execute(EnableAccessibilityServiceRequest("com.pkg/.Svc"), serial = "serial")
        }
    }

    @Test
    fun testValidation() {
        assertThat(EnableAccessibilityServiceRequest("com.pkg/.Svc").validate().success).isTrue()
        assertThat(EnableAccessibilityServiceRequest("").validate().success).isFalse()
    }
}
