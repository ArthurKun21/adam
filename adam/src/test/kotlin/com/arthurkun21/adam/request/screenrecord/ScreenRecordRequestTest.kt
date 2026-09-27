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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenRecordRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testRecordsAndPulls() {
        runBlocking {
            val recording = temp.newFile("recording.mp4")
            recording.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            val destination = temp.newFile("destination.mp4")

            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShellV2 { "screenrecord --time-limit 1 /data/local/tmp/adam_screenrecord.mp4" }
                            .accept()
                            .respondExit(0)
                    }
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectStat { "/data/local/tmp/adam_screenrecord.mp4" }
                        // S_IFREG
                        respondStat(size = recording.length().toInt(), mode = 32768)
                    }
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectRecv { "/data/local/tmp/adam_screenrecord.mp4" }
                            .respondFile(recording)
                            .respondDoneDone()
                    }
                    session {
                        respondOkay()
                        expectShellV2 { "rm -f /data/local/tmp/adam_screenrecord.mp4" }
                            .accept()
                            .respondExit(0)
                    }
                }
            }

            val result = client.execute(
                ScreenRecordRequest(destination, timeLimitSeconds = 1, supportedFeatures = emptyList()),
                serial = "serial",
            )
            assertThat(result).isEqualTo(destination)
            assertThat(destination.readBytes()).isEqualTo(byteArrayOf(1, 2, 3, 4, 5))
        }
    }

    @Test
    fun testKeepsRemoteFileWhenConfigured() {
        runBlocking {
            val recording = temp.newFile("recording.mp4")
            recording.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            val destination = temp.newFile("destination.mp4")

            server.server.multipleSessions {
                serial("serial") {
                    session {
                        respondOkay()
                        expectShellV2 { "screenrecord --time-limit 1 /data/local/tmp/adam_screenrecord.mp4" }
                            .accept()
                            .respondExit(0)
                    }
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectStat { "/data/local/tmp/adam_screenrecord.mp4" }
                        // S_IFREG
                        respondStat(size = recording.length().toInt(), mode = 32768)
                    }
                    session {
                        respondOkay()
                        expectCmd { "sync:" }.accept()
                        expectRecv { "/data/local/tmp/adam_screenrecord.mp4" }
                            .respondFile(recording)
                            .respondDoneDone()
                    }
                }
            }

            client.execute(
                ScreenRecordRequest(
                    destination,
                    timeLimitSeconds = 1,
                    deleteRemoteFile = false,
                    supportedFeatures = emptyList(),
                ),
                serial = "serial",
            )
            assertThat(destination.readBytes()).isEqualTo(byteArrayOf(1, 2, 3, 4, 5))
        }
    }

    @Test
    fun testValidation() {
        val features = emptyList<Feature>()
        assertThat(
            ScreenRecordRequest(File("recording.mp4"), timeLimitSeconds = 180, supportedFeatures = features)
                .validate().success,
        ).isTrue()
        assertThat(
            ScreenRecordRequest(File("recording.mp4"), timeLimitSeconds = 0, supportedFeatures = features)
                .validate().success,
        ).isFalse()
        assertThat(
            ScreenRecordRequest(File("recording.mp4"), timeLimitSeconds = 181, supportedFeatures = features)
                .validate().success,
        ).isFalse()
    }
}
