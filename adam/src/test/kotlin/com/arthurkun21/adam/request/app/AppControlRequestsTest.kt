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
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.Const
import org.junit.Test

class AppControlRequestsTest {
    @Test
    fun testLaunchAppRequestSerialize() {
        val bytes = LaunchAppRequest("com.example").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("004Ashell:monkey -p com.example -c android.intent.category.LAUNCHER 1;echo x$?")
    }

    @Test
    fun testLaunchAppRequestValidation() {
        assertThat(LaunchAppRequest("com.example").validate().success).isTrue()
        assertThat(LaunchAppRequest(" ").validate().success).isFalse()
    }

    @Test
    fun testForceStopAppRequestSerialize() {
        val bytes = ForceStopAppRequest("com.example").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0028shell:am force-stop com.example;echo x$?")
    }

    @Test
    fun testClearAppDataRequestSerialize() {
        val bytes = ClearAppDataRequest("com.example").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0023shell:pm clear com.example;echo x$?")
    }

    @Test
    fun testOpenUrlRequestSerialize() {
        val bytes = OpenUrlRequest("https://example.com").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("004Eshell:am start -a android.intent.action.VIEW -d 'https://example.com';echo x\$?")
    }

    @Test
    fun testOpenUrlRequestValidation() {
        assertThat(OpenUrlRequest("https://example.com").validate().success).isTrue()
        assertThat(OpenUrlRequest("").validate().success).isFalse()
    }
}
