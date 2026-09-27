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
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

/**
 * Blocks until the device reports `sys.boot_completed=1`
 *
 * Wait for [DeviceState.DEVICE] with [WaitForDeviceRequest] first: the shell is unavailable while
 * the device is still booting
 *
 * @param timeoutMs maximum time to wait
 * @param pollIntervalMs interval between boot-completed polls
 * @throws TimeoutException if the device doesn't finish booting in time
 */
public class WaitForBootCompleteRequest(
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : MultiRequest<Unit>() {
    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?,
    ) {
        try {
            withTimeout(timeoutMs) {
                while (true) {
                    val bootCompleted = androidDebugBridgeClient
                        .execute(GetSinglePropRequest(BOOT_COMPLETED_PROP), serial)
                        .trim()
                    if (bootCompleted == "1") {
                        return@withTimeout
                    }
                    delay(pollIntervalMs)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw TimeoutException("Device $serial did not finish booting within ${timeoutMs}ms")
        }
    }

    override fun validate(): ValidationResponse = when {
        timeoutMs <= 0 -> ValidationResponse(false, "timeoutMs must be positive")
        pollIntervalMs <= 0 -> ValidationResponse(false, "pollIntervalMs must be positive")
        else -> ValidationResponse.Success
    }

    public companion object {
        public const val BOOT_COMPLETED_PROP: String = "sys.boot_completed"
        public const val DEFAULT_TIMEOUT_MS: Long = 120_000L
        public const val DEFAULT_POLL_INTERVAL_MS: Long = 500L
    }
}
