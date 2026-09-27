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

package com.arthurkun21.adam.request.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import org.junit.Test

class ForegroundAppRequestTest {
    @Test
    fun testSerialize() {
        val bytes = ForegroundAppRequest().serialize()
        val actual = String(bytes, Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("004Cshell:dumpsys window displays | grep -E 'mCurrentFocus|mFocusedApp';echo x\$?")
    }

    @Test
    fun testParsesCurrentFocus() {
        val output = "  mCurrentFocus=Window{7c7e0a8 u0 com.example.app/com.example.app.MainActivity}\n"
        val result = ForegroundAppRequest().convertResult(ShellCommandResult(output.toByteArray(), 0))
        assertThat(result).isEqualTo(ForegroundApp("com.example.app", "com.example.app.MainActivity"))
    }

    @Test
    fun testParsesFocusedApp() {
        val output = "  mFocusedApp=ActivityRecord{deadbeef u0 com.other.app/.OtherActivity t42}\n"
        val result = ForegroundAppRequest().convertResult(ShellCommandResult(output.toByteArray(), 0))
        assertThat(result).isEqualTo(ForegroundApp("com.other.app", ".OtherActivity"))
    }

    @Test
    fun testFallsBackToFocusedAppWhenSystemWindowFocused() {
        val output = """
            mCurrentFocus=Window{99dd76 u0 NotificationShade}
            mFocusedApp=ActivityRecord{38ddb20 u0 com.android.settings/.Settings t1569}
        """.trimIndent() + "\n"
        val result = ForegroundAppRequest().convertResult(ShellCommandResult(output.toByteArray(), 0))
        assertThat(result).isEqualTo(ForegroundApp("com.android.settings", ".Settings"))
    }

    @Test
    fun testReturnsSystemWindowWithoutPackageAsLastResort() {
        val output = "  mCurrentFocus=Window{99dd76 u0 NotificationShade}\n"
        val result = ForegroundAppRequest().convertResult(ShellCommandResult(output.toByteArray(), 0))
        assertThat(result).isEqualTo(ForegroundApp("NotificationShade", null))
    }

    @Test
    fun testReturnsNullWhenNoMatch() {
        val result = ForegroundAppRequest().convertResult(ShellCommandResult("nothing here\n".toByteArray(), 1))
        assertThat(result).isNull()
    }
}
