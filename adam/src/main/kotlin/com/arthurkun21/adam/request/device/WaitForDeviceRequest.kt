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

package com.arthurkun21.adam.request.device

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.device.ListDevicesRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

/**
 * Blocks until a device with the given serial is connected to the adb server and optionally in the
 * given state
 *
 * @param state required device state or null to accept any state
 * @param timeoutMs maximum time to wait
 * @param pollIntervalMs interval between device list polls
 * @throws TimeoutException if the device doesn't reach the expected state in time
 */
public class WaitForDeviceRequest(
    private val serial: String,
    private val state: DeviceState? = DeviceState.DEVICE,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : MultiRequest<Device>() {
    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?,
    ): Device {
        try {
            return withTimeout(timeoutMs) {
                var matched: Device? = null
                while (matched == null) {
                    matched = androidDebugBridgeClient.execute(ListDevicesRequest())
                        .find { it.serial == this@WaitForDeviceRequest.serial }
                        ?.takeIf { state == null || it.state == state }
                    if (matched == null) {
                        delay(pollIntervalMs)
                    }
                }
                matched
            }
        } catch (e: TimeoutCancellationException) {
            throw TimeoutException(
                "Device ${this@WaitForDeviceRequest.serial} did not reach state $state within ${timeoutMs}ms",
            )
        }
    }

    override fun validate(): ValidationResponse = when {
        this.serial.isBlank() -> ValidationResponse(false, "serial must not be blank")
        timeoutMs <= 0 -> ValidationResponse(false, "timeoutMs must be positive")
        pollIntervalMs <= 0 -> ValidationResponse(false, "pollIntervalMs must be positive")
        else -> ValidationResponse.Success
    }

    public companion object {
        public const val DEFAULT_TIMEOUT_MS: Long = 120_000L
        public const val DEFAULT_POLL_INTERVAL_MS: Long = 500L
    }
}
