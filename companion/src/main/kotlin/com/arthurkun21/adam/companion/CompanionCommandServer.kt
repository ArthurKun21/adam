/*
 * Copyright 2026 Google LLC
 * Portions Copyright (C) 2026 ArthurKun21
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

// Ported to Kotlin from google/artemis packages/artemis-accessibility-helper (Apache 2.0)

package com.arthurkun21.adam.companion

import android.util.Log
import com.arthurkun21.adam.companion.HierarchyDumper.DumpOptions
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Multi-threaded command server bound strictly to loopback (127.0.0.1:18888).
 *
 * Protocols:
 * - HTTP (/ping, /dump, /dump_xml, /hierarchy.xml, /snapshot, /action)
 * - Line-delimited JSON-RPC over a raw TCP socket
 *
 * Every endpoint except `/ping` requires the session token the host pushed
 * through [TokenReceiver] (header `X-Adam-Token`, query parameter `token`, or
 * JSON field `token`). Loopback is reachable by every app on the device, so
 * without the token this service would hand any local app a full-screen reader
 * and a gesture injector.
 *
 * Request bodies are read as bytes (Content-Length is a byte count), so UTF-8
 * payloads of any script are handled exactly.
 */
class CompanionCommandServer(
    private val service: CompanionAccessibilityService,
    private val port: Int,
) : Thread("AdamCompanionCommandServer") {

    @Volatile
    private var isRunning = true

    private var serverSocket: ServerSocket? = null
    private val clientExecutor: ExecutorService = Executors.newCachedThreadPool()

    override fun run() {
        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 50)
            serverSocket = socket
            Log.i(TAG, "CompanionCommandServer listening on 127.0.0.1:$port")

            while (isRunning) {
                val client: Socket = try {
                    socket.accept()
                } catch (e: Exception) {
                    if (!isRunning) break
                    continue
                }

                clientExecutor.execute {
                    handleClient(client)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server error", e)
        } finally {
            serverSocket?.let(::closeQuietly)
        }
    }

    // ------------------------------------------------------------------ //
    // Byte-accurate request reading
    // ------------------------------------------------------------------ //

    /** Reads one line (without CR/LF) as raw bytes; null at end of stream. */
    private fun readLineBytes(input: InputStream): ByteArray? {
        val line = ByteArrayOutputStream(256)
        var b: Int = -1
        while (input.read().also { b = it } >= 0) {
            if (b == '\n'.code) {
                break
            }
            if (b != '\r'.code) {
                line.write(b)
            }
            if (line.size() > MAX_HEADER_LINE) {
                throw IllegalStateException("Header line too long")
            }
        }
        if (b < 0 && line.size() == 0) {
            return null
        }
        return line.toByteArray()
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val buf = ByteArray(length)
        var total = 0
        while (total < length) {
            val r = input.read(buf, total, length - total)
            if (r < 0) break
            total += r
        }
        if (total < length) {
            return buf.copyOf(total)
        }
        return buf
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()

            val firstLine = readLineBytes(input) ?: return
            val ascii = String(firstLine, StandardCharsets.ISO_8859_1).trim()

            when {
                ascii.startsWith("GET ") || ascii.startsWith("POST ") -> handleHttp(ascii, input, output)

                ascii.startsWith("{") ->
                    handleJsonRpc(String(firstLine, StandardCharsets.UTF_8).trim(), output)

                else -> sendJson(output, 400, errorJson("Unsupported protocol").toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client handling error: ${e.message}")
        } finally {
            closeQuietly(socket)
        }
    }

    // ------------------------------------------------------------------ //
    // HTTP
    // ------------------------------------------------------------------ //

    private fun parseQuery(query: String?): Map<String, String> {
        val params = HashMap<String, String>()
        if (query.isNullOrEmpty()) return params
        for (pair in query.split("&")) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            try {
                val key = URLDecoder.decode(if (eq < 0) pair else pair.substring(0, eq), "UTF-8")
                val value = if (eq < 0) "" else URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                params[key] = value
            } catch (ignored: Exception) {
            }
        }
        return params
    }

    private fun handleHttp(requestLine: String, input: InputStream, output: OutputStream) {
        try {
            val parts = requestLine.split(" ")
            val target = if (parts.size > 1) parts[1] else "/"
            val query: Map<String, String>
            val path: String
            val q = target.indexOf('?')
            if (q >= 0) {
                path = target.substring(0, q)
                query = parseQuery(target.substring(q + 1))
            } else {
                path = target
                query = HashMap()
            }

            var contentLength = 0
            var headerToken: String? = null
            while (true) {
                val lineBytes = readLineBytes(input) ?: break
                if (lineBytes.isEmpty()) break
                val line = String(lineBytes, StandardCharsets.ISO_8859_1)
                val colon = line.indexOf(':')
                if (colon < 0) continue
                val name = line.substring(0, colon).trim().lowercase(Locale.ROOT)
                val value = line.substring(colon + 1).trim()
                when (name) {
                    "content-length" ->
                        try {
                            contentLength = value.toInt()
                        } catch (ignored: Exception) {
                        }

                    TOKEN_HEADER -> headerToken = value
                }
            }
            if (contentLength < 0 || contentLength > MAX_BODY) {
                sendJson(output, 413, errorJson("Body too large").toString())
                return
            }

            val body = if (contentLength > 0) {
                String(readExactly(input, contentLength), StandardCharsets.UTF_8)
            } else {
                ""
            }

            val token = headerToken ?: query["token"]
            val authed = TokenStore.matches(token)

            if (path == "/ping" || path == "/") {
                sendJson(output, 200, buildPing(authed).toString())
                return
            }
            if (!authed) {
                sendJson(output, 401, unauthorizedJson().toString())
                return
            }

            when {
                path == "/snapshot" -> {
                    val options = DumpOptions.forSnapshot().applyQuery(query)
                    sendJson(output, 200, HierarchyDumper.dumpAtomicSnapshot(service, options).toString())
                }

                path == "/dump_xml" || path == "/hierarchy.xml" ||
                    (path == "/dump" && query["format"] == "xml") -> {
                    val options = DumpOptions.forSnapshot().applyQuery(query)
                    sendXml(output, HierarchyDumper.dumpXml(service, options))
                }

                path == "/dump" || path == "/hierarchy" -> {
                    val options = DumpOptions.forDump().applyQuery(query)
                    sendJson(output, 200, HierarchyDumper.dump(service, options).toString())
                }

                path == "/action" || path == "/rpc" -> {
                    val json = if (body.isEmpty()) org.json.JSONObject() else org.json.JSONObject(body)
                    val resp = executeCommand(json.optString("cmd", ""), json)
                    sendJson(output, 200, resp.toString())
                }

                else -> sendJson(output, 404, errorJson("Unknown endpoint: $path").toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP error", e)
            try {
                sendJson(output, 500, errorJson("Internal error: ${e.message}").toString())
            } catch (ignored: Throwable) {
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Raw JSON-RPC
    // ------------------------------------------------------------------ //

    private fun handleJsonRpc(line: String, output: OutputStream) {
        try {
            val json = org.json.JSONObject(line)
            val cmd = json.optString("cmd", "")
            val authed = TokenStore.matches(json.optString("token", null))
            val resp: org.json.JSONObject = if (cmd.equals("ping", ignoreCase = true)) {
                buildPing(authed)
            } else if (!authed) {
                unauthorizedJson()
            } else {
                executeCommand(cmd, json)
            }
            output.write(resp.toString().toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
            output.flush()
        } catch (e: Exception) {
            try {
                output.write(errorJson("JSON parse error: ${e.message}").toString().toByteArray(StandardCharsets.UTF_8))
                output.write('\n'.code)
                output.flush()
            } catch (ignored: Exception) {
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Commands
    // ------------------------------------------------------------------ //

    private fun executeCommand(cmd: String, params: org.json.JSONObject): org.json.JSONObject {
        val resp = org.json.JSONObject()
        try {
            when (cmd.lowercase(Locale.ROOT)) {
                "ping" -> return buildPing(true)

                "dump", "dump_ui" ->
                    return HierarchyDumper.dump(service, HierarchyDumper.DumpOptions.forDump())

                "snapshot" ->
                    return HierarchyDumper.dumpAtomicSnapshot(service, HierarchyDumper.DumpOptions.forSnapshot())

                "dump_xml" -> {
                    resp.put("success", true)
                    resp.put("xml", HierarchyDumper.dumpXml(service, HierarchyDumper.DumpOptions.forSnapshot()))
                }

                "tap" -> {
                    val x = params.optDouble("x", -1.0).toFloat()
                    val y = params.optDouble("y", -1.0).toFloat()
                    val tapTimeout = params.optLong("timeout", 1500L)
                    if (x < 0 || y < 0) {
                        resp.put("success", false)
                        resp.put("error", "Invalid coordinates")
                    } else {
                        resp.put("success", GestureController.tap(service, x, y, tapTimeout))
                    }
                }

                "double_tap" -> {
                    val x = params.optDouble("x", -1.0).toFloat()
                    val y = params.optDouble("y", -1.0).toFloat()
                    val timeout = params.optLong("timeout", 2000L)
                    if (x < 0 || y < 0) {
                        resp.put("success", false)
                        resp.put("error", "Invalid coordinates")
                    } else {
                        resp.put("success", GestureController.doubleTap(service, x, y, timeout))
                    }
                }

                "long_press" -> {
                    val x = params.optDouble("x", -1.0).toFloat()
                    val y = params.optDouble("y", -1.0).toFloat()
                    val duration = params.optLong("duration", 1000L)
                    if (x < 0 || y < 0) {
                        resp.put("success", false)
                        resp.put("error", "Invalid coordinates")
                    } else {
                        resp.put("success", GestureController.longPress(service, x, y, duration, 2500L))
                    }
                }

                "swipe" -> {
                    val x1 = params.optDouble("x1", -1.0).toFloat()
                    val y1 = params.optDouble("y1", -1.0).toFloat()
                    val x2 = params.optDouble("x2", -1.0).toFloat()
                    val y2 = params.optDouble("y2", -1.0).toFloat()
                    val duration = params.optLong("duration", 300L)
                    resp.put("success", GestureController.swipe(service, x1, y1, x2, y2, duration, 3000L))
                }

                "type" -> {
                    val text = params.optString("text", "")
                    val append = params.optBoolean("append", false)
                    resp.put("success", GestureController.setText(service, text, append))
                }

                "clear" -> resp.put("success", GestureController.clearText(service))

                "clipboard" -> resp.put(
                    "success",
                    GestureController.setClipboard(service, params.optString("text", "")),
                )

                "global" -> {
                    val action = params.optString("action", "")
                    resp.put("success", GestureController.performGlobalAction(service, action))
                }

                else -> {
                    resp.put("success", false)
                    resp.put("error", "Unknown command: $cmd")
                }
            }
        } catch (e: Exception) {
            try {
                resp.put("success", false)
                resp.put("error", e.message)
            } catch (ignored: Exception) {
            }
        }
        return resp
    }

    /**
     * Health payload shared by GET /ping and the "ping" RPC. The host compares
     * version_code / protocol_version against the bundled APK to decide whether
     * to upgrade, and token_set to decide whether to push its token. The
     * foreground package and activity are only disclosed to an authenticated caller.
     */
    private fun buildPing(authed: Boolean): org.json.JSONObject {
        val r = org.json.JSONObject()
        r.put("success", true)
        r.put("service", "CompanionAccessibilityService")
        r.put("version_code", service.versionCode)
        r.put("version_name", service.versionName)
        r.put("protocol_version", CompanionAccessibilityService.PROTOCOL_VERSION)
        r.put("port", port)
        r.put("auth_required", true)
        r.put("token_set", TokenStore.isSet())
        r.put("authenticated", authed)
        if (authed) {
            r.put("package", service.currentPackageName)
            r.put("activity", service.currentActivityName)
        }
        return r
    }

    private fun errorJson(message: String): org.json.JSONObject {
        val r = org.json.JSONObject()
        r.put("success", false)
        r.put("error", message)
        return r
    }

    private fun unauthorizedJson(): org.json.JSONObject {
        val r = errorJson(
            if (TokenStore.isSet()) {
                "unauthorized: wrong or missing $TOKEN_HEADER"
            } else {
                "unauthorized: no session token has been pushed to the helper yet"
            },
        )
        r.put("token_set", TokenStore.isSet())
        return r
    }

    // ------------------------------------------------------------------ //
    // Responses
    // ------------------------------------------------------------------ //

    private fun reason(status: Int): String = when (status) {
        200 -> "OK"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        404 -> "Not Found"
        413 -> "Payload Too Large"
        else -> "Internal Server Error"
    }

    private fun send(output: OutputStream, status: Int, contentType: String, bytes: ByteArray) {
        try {
            val header = "HTTP/1.1 $status ${reason(status)}\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
            output.write(header.toByteArray(StandardCharsets.UTF_8))
            output.write(bytes)
            output.flush()
        } catch (ignored: Exception) {
        }
    }

    private fun sendJson(output: OutputStream, status: Int, jsonStr: String) {
        send(output, status, "application/json", jsonStr.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendXml(output: OutputStream, xmlStr: String) {
        send(output, 200, "application/xml", xmlStr.toByteArray(StandardCharsets.UTF_8))
    }

    fun shutdown() {
        isRunning = false
        serverSocket?.let(::closeQuietly)
        clientExecutor.shutdownNow()
    }

    private fun closeQuietly(c: Closeable?) {
        try {
            c?.close()
        } catch (ignored: Throwable) {
        }
    }

    private companion object {
        const val TAG = "AdamCompanionCmdSrv"
        const val MAX_HEADER_LINE = 16 * 1024
        const val MAX_BODY = 4 * 1024 * 1024
        const val TOKEN_HEADER = "x-adam-token"
    }
}
