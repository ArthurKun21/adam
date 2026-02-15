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

package com.malinskiy.adam.transport.ktor

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

public class KtorSocketRegressionTest {

    @Test
    public fun readFullyUsesLimitAsLengthForByteArray(): Unit = runBlocking {
        withTimeout(5_000) {
            withSocketPair { clientSocket, serverSocket ->
                val socket = KtorSocket(clientSocket)
                try {
                    val expected = byteArrayOf(11, 22, 33, 44)
                    val writer = async {
                        serverSocket.openWriteChannel(autoFlush = true).writeFully(expected, 0, expected.size)
                        serverSocket.close()
                    }

                    val target = ByteArray(10)
                    socket.readFully(target, 3, expected.size)
                    writer.await()

                    assertThat(target.copyOfRange(0, 3).toList()).isEqualTo(byteArrayOf(0, 0, 0).toList())
                    assertThat(target.copyOfRange(3, 7).toList()).isEqualTo(expected.toList())
                    assertThat(target.copyOfRange(7, 10).toList()).isEqualTo(byteArrayOf(0, 0, 0).toList())
                } finally {
                    socket.close()
                }
            }
        }
    }

    @Test
    public fun writeFullyUsesLimitAsLengthForByteArray(): Unit = runBlocking {
        withTimeout(5_000) {
            withSocketPair { clientSocket, serverSocket ->
                val socket = KtorSocket(clientSocket)
                try {
                    val source = byteArrayOf(7, 8, 9, 10, 11, 12)
                    val reader = async {
                        val buffer = ByteArray(3)
                        serverSocket.openReadChannel().readFully(buffer, 0, buffer.size)
                        buffer
                    }

                    socket.writeFully(source, 2, 3)

                    assertThat(reader.await().toList()).isEqualTo(byteArrayOf(9, 10, 11).toList())
                } finally {
                    socket.close()
                }
            }
        }
    }

    private suspend fun withSocketPair(
        block: suspend (
            clientSocket: io.ktor.network.sockets.Socket,
            serverSocket: io.ktor.network.sockets.Socket,
        ) -> Unit,
    ) {
        val selector = SelectorManager(Dispatchers.IO)
        val server = aSocket(selector).tcp().bind("127.0.0.1", 0)
        try {
            coroutineScope {
                val accepted = async(Dispatchers.IO) { server.accept() }
                val clientSocket = aSocket(selector).tcp().connect(server.localAddress)
                val serverSocket = accepted.await()
                try {
                    block(clientSocket, serverSocket)
                } finally {
                    serverSocket.close()
                }
            }
        } finally {
            server.close()
            selector.close()
        }
    }
}
