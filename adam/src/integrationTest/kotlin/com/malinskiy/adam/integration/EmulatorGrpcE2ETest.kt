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

import com.arthurkun21.adam.emulator.emulatorController
import com.arthurkun21.adam.emulator.emulatorGrpcClient
import com.arthurkun21.adam.emulator.shutdownAndAwaitTermination
import com.google.protobuf.kotlin.Empty
import com.google.protobuf.kotlin.invoke
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class EmulatorGrpcE2ETest {
    @Rule
    @JvmField
    val emulator = AdbDeviceRule(deviceType = DeviceType.EMULATOR)

    @Test
    fun testProto() {
        runBlocking {
            val grpcAddress = emulator.emulatorGrpcAddress()
            Assume.assumeTrue("Emulator gRPC is not available at $grpcAddress", grpcAddress.canConnect())

            val client = emulatorGrpcClient(grpcAddress.hostString, grpcAddress.port)
            try {
                val emulatorController = client.emulatorController()
                val status = emulatorController.getStatus(Empty { })
                println(status)
            } finally {
                client.shutdownAndAwaitTermination()
            }
        }
    }

    private fun AdbDeviceRule.emulatorGrpcAddress(): InetSocketAddress {
        val consoleAddress = if (deviceSerial.startsWith("emulator-")) {
            InetSocketAddress("localhost", deviceSerial.substringAfter('-').toInt())
        } else {
            InetSocketAddress(deviceSerial.substringBeforeLast(':'), deviceSerial.substringAfterLast(':').toInt() - 1)
        }
        return InetSocketAddress(consoleAddress.hostString, consoleAddress.port + 3000)
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
