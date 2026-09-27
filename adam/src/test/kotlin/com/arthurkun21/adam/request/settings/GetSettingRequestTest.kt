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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class GetSettingRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        val bytes = GetSettingRequest(SettingNamespace.SECURE, "myKey").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0028shell:settings get secure myKey;echo x\$?")
    }

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectShell { "settings get secure myKey;echo x\$?" }
                    .accept()
                    .respond("1\nx0")
            }

            val output = client.execute(GetSettingRequest(SettingNamespace.SECURE, "myKey"), serial = "serial")
            assertThat(output).isEqualTo("1")
        }
    }
}
