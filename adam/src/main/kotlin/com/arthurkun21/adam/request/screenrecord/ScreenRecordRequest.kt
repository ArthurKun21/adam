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

package com.arthurkun21.adam.request.screenrecord

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.sync.PullRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Records the device screen with the on-device `screenrecord` command and pulls the resulting
 * mp4 file.
 *
 * The recording runs for [timeLimitSeconds] or until the coroutine executing this request is
 * cancelled — closing the shell channel makes the device finalize the recording (SIGINT/SIGHUP),
 * so cancelling early still produces a valid file. The pull always happens, even on cancellation
 *
 * `screenrecord` caps a single recording at 180 seconds
 *
 * @param destination local file to pull the recording into
 * @param remotePath temporary on-device location of the recording
 * @param timeLimitSeconds recording duration cap (device hard limit: 180)
 * @param deleteRemoteFile remove the recording from the device after pulling
 */
@Features(Feature.SHELL_V2)
public class ScreenRecordRequest(
    private val destination: File,
    private val remotePath: String = DEFAULT_REMOTE_PATH,
    private val timeLimitSeconds: Int = 180,
    private val deleteRemoteFile: Boolean = true,
    private val supportedFeatures: List<Feature>,
) : MultiRequest<File>() {
    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?,
    ): File {
        try {
            with(androidDebugBridgeClient) {
                execute(
                    ShellCommandRequest(
                        cmd = "screenrecord --time-limit $timeLimitSeconds $remotePath",
                        socketIdleTimeout = (timeLimitSeconds + IDLE_TIMEOUT_BUFFER_SECONDS) * 1000,
                    ),
                    serial,
                )
            }
        } finally {
            withContext(NonCancellable) {
                with(androidDebugBridgeClient) {
                    execute(PullRequest(remotePath, destination, supportedFeatures), serial)
                    if (deleteRemoteFile) {
                        execute(ShellCommandRequest("rm -f $remotePath"), serial)
                    }
                }
            }
        }
        return destination
    }

    override fun validate(): ValidationResponse = when {
        timeLimitSeconds !in 1..180 -> {
            ValidationResponse(false, "timeLimitSeconds must be in 1..180 (screenrecord hard limit)")
        }

        remotePath.length >= Const.MAX_REMOTE_PATH_LENGTH -> {
            ValidationResponse(false, "Remote path should be less than ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        }

        else -> ValidationResponse.Success
    }

    public companion object {
        public const val DEFAULT_REMOTE_PATH: String = "/data/local/tmp/adam_screenrecord.mp4"
        private const val IDLE_TIMEOUT_BUFFER_SECONDS = 60L
    }
}
