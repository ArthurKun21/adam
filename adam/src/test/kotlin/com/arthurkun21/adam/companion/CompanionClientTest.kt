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

package com.arthurkun21.adam.companion

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isTrue
import com.arthurkun21.adam.transport.ktor.KtorSocketFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Runs a fake companion helper HTTP server on loopback and asserts the wire contract of
 * [CompanionClient]
 */
class CompanionClientTest {
    private lateinit var serverSocket: ServerSocket
    private val executor = Executors.newCachedThreadPool()
    private val requests = mutableListOf<String>()
    private var handler: (request: String) -> String = { "{}" }

    private lateinit var client: CompanionClient

    fun readHttpRequest(input: java.io.InputStream): String {
        val header = ByteArrayOutputStream()
        var prev1 = -1
        var prev2 = -1
        while (true) {
            val b = input.read()
            if (b < 0) break
            header.write(b)
            if (prev2 == '\r'.code && prev1 == '\n'.code && b == '\r'.code) {
                // one more \n follows
                header.write(input.read())
                break
            }
            prev2 = prev1
            prev1 = b
        }
        val headerText = header.toString("UTF-8")
        val contentLength = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE)
            .find(headerText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        if (contentLength > 0) {
            val body = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val r = input.read(body, read, contentLength - read)
                if (r < 0) break
                read += r
            }
            return headerText + String(body, 0, read, Charsets.UTF_8)
        }
        return headerText
    }

    @Before
    fun setup() {
        serverSocket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        executor.execute {
            while (!serverSocket.isClosed) {
                val socket: Socket = try {
                    serverSocket.accept()
                } catch (e: Exception) {
                    return@execute
                }
                executor.execute {
                    try {
                        socket.soTimeout = 5000
                        val text = readHttpRequest(socket.getInputStream())
                        synchronized(requests) { requests.add(text) }
                        val status = if (text.contains("X-Adam-Token: secret")) 200 else 401
                        val body = if (status == 200) handler(text) else "{\"error\":\"unauthorized\"}"
                        val response = "HTTP/1.1 $status ${if (status == 200) "OK" else "Unauthorized"}\r\n" +
                            "Content-Type: application/json; charset=utf-8\r\n" +
                            "Content-Length: ${body.encodeToByteArray().size}\r\n" +
                            "Connection: close\r\n\r\n$body"
                        socket.getOutputStream().write(response.toByteArray())
                        socket.getOutputStream().flush()
                    } catch (ignored: Exception) {
                    } finally {
                        try {
                            socket.close()
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }

        client = CompanionClient(
            port = serverSocket.localPort,
            token = "secret",
            socketFactory = KtorSocketFactory(),
        )
    }

    @After
    fun teardown() {
        serverSocket.close()
        executor.shutdownNow()
    }

    @Test
    fun testPing() {
        handler =
            { "{\"success\":true,\"protocol_version\":2,\"port\":18888,\"token_set\":true,\"authenticated\":true}" }
        val ping = runBlocking { client.ping() }

        assertThat(ping.success).isTrue()
        assertThat(ping.protocolVersion).isEqualTo(2)
        assertThat(ping.tokenSet).isTrue()
        synchronized(requests) {
            assertThat(requests.last()).contains("GET /ping HTTP/1.1")
            assertThat(requests.last()).contains("X-Adam-Token: secret")
            assertThat(requests.last()).contains("Connection: close")
        }
    }

    @Test
    fun testUnauthorized() {
        client = CompanionClient(port = serverSocket.localPort, token = "wrong", socketFactory = KtorSocketFactory())
        handler = { "{\"success\":true}" }

        try {
            runBlocking { client.ping() }
            throw AssertionError("Expected CompanionException")
        } catch (expected: CompanionException) {
        }
    }

    @Test
    fun testTapAction() {
        handler = { request ->
            if (request.contains("\"cmd\":\"tap\"")) {
                "{\"success\":true}"
            } else {
                "{\"success\":false,\"error\":\"unexpected\"}"
            }
        }

        val result = runBlocking { client.tap(100, 200) }

        assertThat(result.success).isTrue()
        synchronized(requests) {
            assertThat(requests.last()).contains("POST /action HTTP/1.1")
            assertThat(requests.last()).contains("\"cmd\":\"tap\"")
            assertThat(requests.last()).contains("\"x\":100")
            assertThat(requests.last()).contains("\"y\":200")
        }
    }

    @Test
    fun testTypeAction() {
        handler = { request ->
            if (request.contains("\"cmd\":\"type\"") && request.contains("\"append\":true")) {
                "{\"success\":true}"
            } else {
                "{\"success\":false,\"error\":\"unexpected\"}"
            }
        }

        val result = runBlocking { client.type("héllo \"world\"", append = true) }

        assertThat(result.success).isTrue()
    }

    @Test
    fun testSnapshot() {
        handler = {
            "{\"success\":true,\"rotation\":0,\"width\":1080,\"height\":2400," +
                "\"xml\":\"<hierarchy rotation=\\\"0\\\" />\",\"has_screenshot\":true," +
                "\"screenshot_base64\":\"aGVsbG8=\",\"node_count\":42,\"package\":\"com.example\"}"
        }

        val snapshot = runBlocking { client.snapshot() }

        assertThat(snapshot.success).isTrue()
        assertThat(snapshot.xml).isEqualTo("<hierarchy rotation=\"0\" />")
        assertThat(snapshot.screenshotBase64).isEqualTo("aGVsbG8=")
        assertThat(snapshot.nodeCount).isEqualTo(42)
        assertThat(snapshot.packageName).isEqualTo("com.example")
    }

    @Test
    fun testDumpXml() {
        handler = { "<hierarchy rotation=\"0\" />" }

        val xml = runBlocking { client.dumpXml() }

        assertThat(xml).isEqualTo("<hierarchy rotation=\"0\" />")
    }
}
