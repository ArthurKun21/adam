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

package com.arthurkun21.adam.transport.ktor

import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.SocketFactory
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import io.ktor.network.sockets.InetSocketAddress as KtorInetSocketAddress

public class KtorSocketFactory(
    private val connectTimeout: Long = 10_000,
    private val idleTimeout: Long = 30_000,
) : SocketFactory {
    private val initialized = AtomicBoolean(false)
    private val selectorManager: SelectorManager by lazy {
        SelectorManager(Dispatchers.IO).also { initialized.set(true) }
    }

    override suspend fun tcp(socketAddress: InetSocketAddress, connectTimeout: Long?, idleTimeout: Long?): Socket {
        val address = KtorInetSocketAddress(socketAddress.hostName, socketAddress.port)
        val ktorSocket = try {
            withTimeout(connectTimeout ?: this@KtorSocketFactory.connectTimeout) {
                aSocket(selectorManager)
                    .tcp()
                    .connect(address) {
                        socketTimeout = idleTimeout ?: this@KtorSocketFactory.idleTimeout
                    }
            }
        } catch (e: TimeoutCancellationException) {
            throw SocketTimeoutException("Timed out connecting to $socketAddress")
        }
        return KtorSocket(ktorSocket)
    }

    override fun close() {
        if (initialized.get()) {
            selectorManager.close()
        }
    }
}
