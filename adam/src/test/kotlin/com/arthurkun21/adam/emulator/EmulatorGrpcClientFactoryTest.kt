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

package com.arthurkun21.adam.emulator

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EmulatorGrpcClientFactoryTest {
    /**
     * Regression guard for the gRPC-Java transport provider discovery: the channel builder eagerly
     * resolves a transport via service loader, so this fails with
     * "No functional channel service provider found" if no provider (grpc-okhttp) is on the
     * classpath. No connection is established (lazy on first RPC).
     */
    @Test
    fun testCreatesPlaintextClientWithAvailableTransport() {
        val client = emulatorGrpcClient("127.0.0.1", 1)
        runBlocking {
            client.shutdownAndAwaitTermination(1.seconds)
        }
    }

    @Test
    fun testServiceProxyIsObtained() {
        val client = emulatorGrpcClient("127.0.0.1", 1)
        client.emulatorController()
        runBlocking {
            client.shutdownAndAwaitTermination(1.seconds)
        }
    }
}
