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

import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.SocketFactory
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Exception thrown when the on-device companion helper rejects or cannot serve a request
 */
public open class CompanionException(message: String) : RuntimeException(message)

/**
 * Minimal HTTP/1.1 client for the on-device companion accessibility service.
 *
 * The helper serves a small HTTP subset on loopback; the host reaches it through an adb port
 * forward (usually allocated by [CompanionManager.deploy]). Requests are answered with
 * `Connection: close`, so each call reads the response to EOF over its own socket from
 * adam's [SocketFactory]
 *
 * Endpoints: `/ping` (health), `/snapshot` (atomic hierarchy + screenshot), `/dump_xml`
 * (uiautomator-format hierarchy), `/action` (tap/swipe/type/clipboard/global actions)
 */
public class CompanionClient(
    public val port: Int,
    public val token: String? = null,
    public val host: String = "127.0.0.1",
    private val socketFactory: SocketFactory,
    private val socketIdleTimeout: Long? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    public suspend fun ping(): CompanionPing =
        executeJson("GET", "/ping", null, CompanionPing.serializer())

    public suspend fun dumpXml(): String = execute("GET", "/dump_xml", null).second

    public suspend fun snapshot(): CompanionSnapshot =
        executeJson("GET", "/snapshot", null, CompanionSnapshot.serializer())

    public suspend fun tap(x: Int, y: Int): CompanionActionResult =
        action("tap", mapOf("x" to x, "y" to y))

    public suspend fun doubleTap(x: Int, y: Int): CompanionActionResult =
        action("double_tap", mapOf("x" to x, "y" to y))

    public suspend fun longPress(x: Int, y: Int, durationMs: Long = 1000): CompanionActionResult =
        action("long_press", mapOf("x" to x, "y" to y, "duration" to durationMs))

    public suspend fun swipe(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        durationMs: Long = 300,
    ): CompanionActionResult =
        action("swipe", mapOf("x1" to x1, "y1" to y1, "x2" to x2, "y2" to y2, "duration" to durationMs))

    public suspend fun type(text: String, append: Boolean = false): CompanionActionResult =
        action("type", mapOf("text" to text, "append" to append))

    public suspend fun clearText(): CompanionActionResult = action("clear", emptyMap())

    public suspend fun setClipboard(text: String): CompanionActionResult =
        action("clipboard", mapOf("text" to text))

    public suspend fun globalAction(action: String): CompanionActionResult =
        action("global", mapOf("action" to action))

    private suspend fun action(cmd: String, params: Map<String, Any?>): CompanionActionResult {
        val body = buildJsonObject {
            put("cmd", JsonPrimitive(cmd))
            params.forEach { (key, value) ->
                put(
                    key,
                    when (value) {
                        is String -> JsonPrimitive(value)
                        is Boolean -> JsonPrimitive(value)
                        is Number -> JsonPrimitive(value)
                        else -> JsonPrimitive(value.toString())
                    },
                )
            }
        }.toString()
        return executeJson("POST", "/action", body, CompanionActionResult.serializer())
    }

    private suspend fun <T> executeJson(
        method: String,
        path: String,
        body: String?,
        serializer: KSerializer<T>,
    ): T {
        val (status, payload) = execute(method, path, body)
        if (status != 200) {
            throw CompanionException("Companion helper returned HTTP $status for $path: ${payload.take(200)}")
        }
        return json.decodeFromString(serializer, payload)
    }

    /**
     * @return HTTP status and the response body
     */
    private suspend fun execute(method: String, path: String, body: String?): Pair<Int, String> {
        val address = InetSocketAddress(InetAddress.getByName(host), port)
        return socketFactory.tcp(address, idleTimeout = socketIdleTimeout).use { socket ->
            val payload = buildString {
                append("$method $path HTTP/1.1\r\n")
                append("Host: $host:$port\r\n")
                token?.let { append("X-Adam-Token: $it\r\n") }
                if (body != null) {
                    val bytes = body.encodeToByteArray()
                    append("Content-Type: application/json\r\n")
                    append("Content-Length: ${bytes.size}\r\n")
                }
                append("Connection: close\r\n\r\n")
            }
            socket.writeFully(payload.encodeToByteArray())
            body?.let { socket.writeFully(it.encodeToByteArray()) }

            val raw = readToEnd(socket)
            val headerEnd = raw.indexOf("\r\n\r\n")
            if (headerEnd < 0) {
                throw CompanionException("Malformed response from companion helper: ${raw.take(200)}")
            }
            val statusLine = raw.substringBefore("\r\n")
            val status = statusLine.split(" ").getOrNull(1)?.toIntOrNull()
                ?: throw CompanionException("Malformed status line: $statusLine")
            status to raw.substring(headerEnd + 4)
        }
    }

    private suspend fun readToEnd(socket: Socket): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        loop@ while (true) {
            val available = socket.readAvailable(buffer, 0, buffer.size)
            when {
                available < 0 -> break@loop

                available > 0 -> {
                    output.write(buffer, 0, available)
                    yield()
                }

                else -> yield()
            }
        }
        return output.toString("UTF-8")
    }

    public companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
