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

package com.malinskiy.adam.integration

import assertk.assertThat
import assertk.assertions.startsWith
import com.malinskiy.adam.request.emu.EmulatorCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class EmulatorE2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(deviceType = DeviceType.EMULATOR)

    @Test
    fun testHelpCommand() {
        runBlocking {
            val address = adbRule.emulatorConsoleAddress()
            Assume.assumeTrue("Emulator console is not available at $address", address.canConnect())

            val output = adbRule.adb.execute(
                EmulatorCommandRequest(
                    "help",
                    address,
                ),
            )
            assertThat(output).startsWith("Android console commands")
        }
    }

    private fun AdbDeviceRule.emulatorConsoleAddress(): InetSocketAddress {
        return if (deviceSerial.startsWith("emulator-")) {
            InetSocketAddress("localhost", deviceSerial.substringAfter('-').toInt())
        } else {
            InetSocketAddress(deviceSerial.substringBeforeLast(':'), deviceSerial.substringAfterLast(':').toInt() - 1)
        }
    }

    private fun InetSocketAddress.canConnect(): Boolean {
        return try {
            Socket().use { socket -> socket.connect(this, CONNECT_TIMEOUT_MS) }
            true
        } catch (e: IOException) {
            false
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 1000
    }
}
