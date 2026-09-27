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

import com.arthurkun21.adam.request.settings.AmBroadcastRequest
import com.arthurkun21.adam.request.settings.DisableAccessibilityServiceRequest
import com.arthurkun21.adam.request.settings.EnableAccessibilityServiceRequest
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardRequest
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import com.malinskiy.adam.request.forwarding.RemovePortForwardRequest
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.PushRequest
import kotlinx.coroutines.delay
import java.io.File
import java.security.SecureRandom

/**
 * An active companion helper deployment: a connected [CompanionClient] plus the means to tear
 * the deployment down
 */
public class CompanionDeployment internal constructor(
    public val client: CompanionClient,
    public val serial: String,
    public val token: String,
    private val manager: CompanionManager,
) {
    /**
     * Removes the adb port forward. With [disableService] the accessibility service is also
     * disabled, with [uninstall] the helper APK is removed from the device
     */
    public suspend fun teardown(
        disableService: Boolean = false,
        uninstall: Boolean = false,
    ) {
        manager.teardown(this, disableService, uninstall)
    }
}

/**
 * Deploys and manages the on-device companion accessibility service.
 *
 * The helper is a non-root companion service that provides real gesture injection, text entry,
 * multi-window UI hierarchy dumps and atomic screenshots on real devices, over a
 * token-authenticated loopback HTTP server reached via an adb port forward
 *
 * Deployment flow ([deploy]):
 * 1. push the APK to the device and install it (`pm install -r -g`)
 * 2. enable the accessibility service via secure settings (no user interaction needed)
 * 3. push a fresh session token via an `am broadcast` only the adb shell user may deliver
 * 4. allocate a local port with `adb forward tcp:0 tcp:18888`
 * 5. poll `/ping` until the service reports protocol version [CompanionConstants.PROTOCOL_VERSION]
 *
 * @param supportedFeatures features of the target device, see
 * [com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest]
 */
public class CompanionManager(
    private val client: AndroidDebugBridgeClient,
    private val supportedFeatures: List<Feature>,
    private val remoteApkPath: String = DEFAULT_REMOTE_APK_PATH,
    private val pingAttempts: Int = DEFAULT_PING_ATTEMPTS,
    private val pingIntervalMs: Long = DEFAULT_PING_INTERVAL_MS,
) {
    private val random = SecureRandom()

    /**
     * Deploys the helper APK and establishes a live connection
     *
     * @param apk the companion helper APK built by the `:companion` module
     * (`companion/build/outputs/apk/debug/debug.apk`)
     * @param token session token presented to the helper over HTTP. A fresh 48-character hex
     * token is generated when omitted
     * @param serial device serial; required for the port forward
     * @throws CompanionException when the service doesn't become ready in time
     */
    public suspend fun deploy(serial: String, apk: File, token: String = newToken()): CompanionDeployment {
        with(client) {
            execute(PushRequest(apk, remoteApkPath, supportedFeatures), serial)
            execute(InstallRemotePackageRequest(remoteApkPath, reinstall = true, extraArgs = listOf("-g")), serial)
            execute(EnableAccessibilityServiceRequest(CompanionConstants.SERVICE_COMPONENT), serial)
            execute(
                AmBroadcastRequest(
                    action = CompanionConstants.ACTION_SET_TOKEN,
                    component = CompanionConstants.RECEIVER_COMPONENT,
                    stringExtras = mapOf(CompanionConstants.EXTRA_TOKEN to token),
                ),
                serial,
            )

            val localPort = execute(
                PortForwardRequest(
                    local = LocalTcpPortSpec(0),
                    remote = RemoteTcpPortSpec(CompanionConstants.DEVICE_PORT),
                    serial = serial,
                ),
                serial,
            ) ?: throw CompanionException("adb did not report the forwarded port")

            val companion = CompanionClient(
                port = localPort,
                token = token,
                socketFactory = client.socketFactory,
            )

            var lastError: Exception? = null
            repeat(pingAttempts) {
                try {
                    val ping = companion.ping()
                    if (ping.protocolVersion >= CompanionConstants.PROTOCOL_VERSION) {
                        return CompanionDeployment(companion, serial, token, this@CompanionManager)
                    }
                    lastError = CompanionException(
                        "Companion helper protocol ${ping.protocolVersion} < ${CompanionConstants.PROTOCOL_VERSION}",
                    )
                } catch (e: Exception) {
                    lastError = e
                }
                delay(pingIntervalMs)
            }
            throw lastError ?: CompanionException("Companion helper did not become ready")
        }
    }

    internal suspend fun teardown(
        deployment: CompanionDeployment,
        disableService: Boolean,
        uninstall: Boolean,
    ) {
        if (disableService) {
            client.execute(DisableAccessibilityServiceRequest(CompanionConstants.SERVICE_COMPONENT), deployment.serial)
        }
        if (uninstall) {
            client.execute(
                ShellCommandRequest("pm uninstall ${CompanionConstants.APPLICATION_ID}"),
                deployment.serial,
            )
        }
        client.execute(
            RemovePortForwardRequest(LocalTcpPortSpec(deployment.client.port), deployment.serial),
            deployment.serial,
        )
    }

    /**
     * Generates a fresh 48-character hex session token (24 random bytes)
     */
    public fun newToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    public companion object {
        public const val DEFAULT_REMOTE_APK_PATH: String = "/data/local/tmp/adam_companion.apk"
        public const val DEFAULT_PING_ATTEMPTS: Int = 24
        public const val DEFAULT_PING_INTERVAL_MS: Long = 500L
    }
}
