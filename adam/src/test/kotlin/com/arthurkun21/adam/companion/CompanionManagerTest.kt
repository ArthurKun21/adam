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
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.arthurkun21.adam.transport.ktor.KtorSocketFactory
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Verifies the full companion deployment flow against the adb server stub and a fake helper
 * HTTP server: push + install APK, enable the accessibility service, push the token, allocate
 * a random local port via `adb forward tcp:0` and wait for /ping
 */
class CompanionManagerTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    private val token = "0123456789abcdef0123456789abcdef0123456789abcdef"
    private val remoteApkPath = "/data/local/tmp/adam_companion.apk"
    private var helperSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    private fun readHttpRequest(input: java.io.InputStream): String {
        val header = ByteArrayOutputStream()
        var prev1 = -1
        var prev2 = -1
        while (true) {
            val b = input.read()
            if (b < 0) break
            header.write(b)
            if (prev2 == '\r'.code && prev1 == '\n'.code && b == '\r'.code) {
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

    @After
    fun teardown() {
        helperSocket?.close()
        executor.shutdownNow()
    }

    private fun startHelper() {
        val server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        helperSocket = server
        executor.execute {
            while (!server.isClosed) {
                val clientSocket = try {
                    server.accept()
                } catch (e: Exception) {
                    return@execute
                }
                executor.execute {
                    try {
                        readHttpRequest(clientSocket.getInputStream())
                        val body = "{\"success\":true,\"protocol_version\":2,\"token_set\":true,\"authenticated\":true}"
                        val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=utf-8\r\n" +
                            "Content-Length: ${body.encodeToByteArray().size}\r\n" +
                            "Connection: close\r\n\r\n$body"
                        clientSocket.getOutputStream().write(response.toByteArray())
                        clientSocket.getOutputStream().flush()
                    } catch (ignored: Exception) {
                    } finally {
                        try {
                            clientSocket.close()
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testDeployFlow() {
        runBlocking {
            startHelper()
            val apk = temp.newFile("companion.apk")
            apk.writeBytes(ByteArray(1024) { it.toByte() })
            val receivedApk = temp.newFile("received.apk")

            server.server.multipleSessions {
                serial("serial") {
                    // PushRequest: stat the source
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectStat { remoteApkPath }
                        // S_IFREG
                        respondStat(size = apk.length().toInt(), mode = 32768)
                    }
                    // PushRequest: send the file
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectSend { "$remoteApkPath,511" }
                            .receiveFile(receivedApk)
                            .done()
                    }
                    // InstallRemotePackageRequest
                    session {
                        respondOkay()
                        expectShell { "pm install -r -g $remoteApkPath;echo x$?" }
                            .accept()
                            .respond("Success\nx0")
                    }
                    // EnableAccessibilityServiceRequest: read current services
                    session {
                        respondOkay()
                        expectShell { "settings get secure enabled_accessibility_services;echo x$?" }
                            .accept()
                            .respond("null\nx0")
                    }
                    // EnableAccessibilityServiceRequest: write the service list
                    session {
                        respondOkay()
                        expectShell {
                            "settings put secure 'enabled_accessibility_services' " +
                                "'com.arthurkun21.adam.companion/.CompanionAccessibilityService';echo x$?"
                        }
                            .accept()
                            .respond("x0")
                    }
                    // EnableAccessibilityServiceRequest: enable accessibility
                    session {
                        respondOkay()
                        expectShell { "settings put secure 'accessibility_enabled' '1';echo x$?" }
                            .accept()
                            .respond("x0")
                    }
                    // AmBroadcastRequest: push the session token
                    session {
                        respondOkay()
                        expectShell {
                            "am broadcast -a com.arthurkun21.adam.companion.SET_TOKEN " +
                                "-n com.arthurkun21.adam.companion/.TokenReceiver " +
                                "--es 'token' '$token';echo x$?"
                        }
                            .accept()
                            .respond("x0")
                    }
                    // PortForwardRequest with tcp:0 random port allocation
                    session {
                        respondOkay()
                        expectCmd { "host-serial:serial:forward:tcp:0;tcp:18888" }.accept()
                        // the request reads the transport response twice: handshake + readElement
                        respondOkay()
                        output.respondStringV1(helperSocket?.localPort.toString())
                    }
                    // teardown: killforward of the allocated port
                    session {
                        respondOkay()
                        expectCmd { "host-serial:serial:killforward:tcp:${helperSocket?.localPort}" }.accept()
                    }
                }
            }

            val manager = CompanionManager(
                client = client,
                supportedFeatures = emptyList(),
                pingAttempts = 5,
                pingIntervalMs = 100,
            )
            val deployment = manager.deploy("serial", apk, token)

            assertThat(deployment.client.port).isEqualTo(helperSocket?.localPort)
            assertThat(deployment.client.token).isEqualTo(token)
            assertThat(deployment.serial).isEqualTo("serial")

            // teardown removes the forward
            deployment.teardown()
        }
    }

    @Test
    fun testNewTokenLength() {
        val manager = CompanionManager(client, emptyList())
        val token = manager.newToken()

        assertThat(token.length).isEqualTo(48)
        assertThat(token.all { it in '0'..'9' || it in 'a'..'f' }).isTrue()
    }
}
