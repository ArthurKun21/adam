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

package com.arthurkun21.adam

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import com.arthurkun21.adam.request.app.ForceStopAppRequest
import com.arthurkun21.adam.request.app.ForegroundAppRequest
import com.arthurkun21.adam.request.app.LaunchAppRequest
import com.arthurkun21.adam.request.control.AndroidKeyCode
import com.arthurkun21.adam.request.control.InputKeyEventRequest
import com.arthurkun21.adam.request.control.InputSwipeRequest
import com.arthurkun21.adam.request.control.InputTapRequest
import com.arthurkun21.adam.request.control.ScreencapRequest
import com.arthurkun21.adam.request.control.UiAutomatorHierarchyRequest
import com.arthurkun21.adam.request.device.GetStateRequest
import com.arthurkun21.adam.request.device.WaitForBootCompleteRequest
import com.arthurkun21.adam.request.device.WaitForDeviceRequest
import com.arthurkun21.adam.request.screenrecord.ScreenRecordRequest
import com.arthurkun21.adam.request.settings.GetSettingRequest
import com.arthurkun21.adam.request.settings.PutSettingRequest
import com.arthurkun21.adam.request.settings.SettingNamespace
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

class ControlE2ETest {
    @get:Rule
    val timeoutRule = Timeout(120_000)

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Rule
    @JvmField
    val adb = AdbDeviceRule(DeviceType.ANY, Feature.SHELL_V2)
    val client = adb.adb

    @Test
    fun testScreencap() {
        runBlocking {
            val png = client.execute(ScreencapRequest(), adb.deviceSerial)
            assertThat(png.size).isGreaterThan(1000)
            // PNG magic
            assertThat(png.copyOfRange(0, 4).toList()).isEqualTo(listOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        }
    }

    @Test
    fun testInput() {
        runBlocking {
            client.execute(InputTapRequest(100, 100), adb.deviceSerial)
            client.execute(InputSwipeRequest(200, 200, 300, 400, 300), adb.deviceSerial)
            client.execute(InputKeyEventRequest(AndroidKeyCode.BACK), adb.deviceSerial)
        }
    }

    @Test
    fun testLaunchStopForeground() {
        runBlocking {
            client.execute(LaunchAppRequest("com.android.settings"), adb.deviceSerial)
            delay(3_000)

            val foreground = client.execute(ForegroundAppRequest(), adb.deviceSerial)
            assertThat(foreground).isNotNull()
            assertThat(foreground!!.packageName).isEqualTo("com.android.settings")

            client.execute(ForceStopAppRequest("com.android.settings"), adb.deviceSerial)
        }
    }

    @Test
    fun testGetState() {
        runBlocking {
            val state = client.execute(GetStateRequest(adb.deviceSerial))
            assertThat(state).isEqualTo(DeviceState.DEVICE)
        }
    }

    @Test
    fun testWaitForDeviceAndBoot() {
        runBlocking {
            val device = client.execute(
                WaitForDeviceRequest(adb.deviceSerial, DeviceState.DEVICE, timeoutMs = 10_000, pollIntervalMs = 250),
            )
            assertThat(device.serial).isEqualTo(adb.deviceSerial)

            client.execute(WaitForBootCompleteRequest(timeoutMs = 10_000, pollIntervalMs = 250), adb.deviceSerial)
        }
    }

    @Test
    fun testUiAutomatorHierarchy() {
        runBlocking {
            val xml = client.execute(UiAutomatorHierarchyRequest(), adb.deviceSerial)
            assertThat(xml).contains("<hierarchy")
        }
    }

    @Test
    fun testScreenRecord() {
        runBlocking {
            val destination = temp.newFile("recording.mp4")
            client.execute(
                ScreenRecordRequest(
                    destination = destination,
                    timeLimitSeconds = 1,
                    supportedFeatures = listOf(Feature.SHELL_V2),
                ),
                adb.deviceSerial,
            )
            // some devices/displays don't support screenrecord (INVALID_LAYER_STACK) - skip there
            Assume.assumeTrue("screenrecord not supported on this device", destination.length() > 1024)
        }
    }

    @Test
    fun testSettingsRoundtrip() {
        runBlocking {
            val key = "adam_e2e_test_key"
            client.execute(PutSettingRequest(SettingNamespace.SECURE, key, "42"), adb.deviceSerial)
            val value = client.execute(GetSettingRequest(SettingNamespace.SECURE, key), adb.deviceSerial)
            assertThat(value).isEqualTo("42")

            // cleanup
            client.execute(ShellCommandRequest("settings delete secure $key"), adb.deviceSerial)
        }
    }
}
