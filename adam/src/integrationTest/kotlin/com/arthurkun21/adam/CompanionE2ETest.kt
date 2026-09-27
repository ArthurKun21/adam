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
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.arthurkun21.adam.companion.CompanionManager
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.io.File

/**
 * Deploys the real companion helper APK to the device, reads the hierarchy, and removes it again
 */
class CompanionE2ETest {
    @get:Rule
    val timeoutRule = Timeout(180_000)

    @Rule
    @JvmField
    val adb = AdbDeviceRule(DeviceType.ANY, Feature.SHELL_V2)
    val client = adb.adb

    @Test
    fun testDeploySnapshotTeardown() {
        runBlocking {
            val apk = File("../companion/build/outputs/apk/debug/companion-debug.apk")
            Assume.assumeTrue("companion APK not built", apk.isFile)

            val manager = CompanionManager(client, adb.supportedFeatures)
            val deployment = manager.deploy(adb.deviceSerial, apk)
            try {
                val ping = deployment.client.ping()
                assertThat(ping.authenticated).isTrue()
                assertThat(ping.protocolVersion).isEqualTo(2)

                val xml = deployment.client.dumpXml()
                assertThat(xml).contains("<hierarchy")

                val snapshot = deployment.client.snapshot()
                assertThat(snapshot.success).isTrue()
                assertThat(snapshot.xml).contains("<hierarchy")
            } finally {
                deployment.teardown(disableService = true, uninstall = true)
            }

            // helper is gone again
            val installed = client.execute(
                com.malinskiy.adam.request.pkg.PmListRequest(),
                adb.deviceSerial,
            ).any { it.name == "com.arthurkun21.adam.companion" }
            assertThat(installed).isFalse()
        }
    }
}
