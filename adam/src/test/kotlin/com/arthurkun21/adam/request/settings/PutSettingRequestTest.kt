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

package com.arthurkun21.adam.request.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.Const
import org.junit.Test

class PutSettingRequestTest {
    @Test
    fun testSerialize() {
        val bytes = PutSettingRequest(SettingNamespace.SECURE, "myKey", "myValue").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0034shell:settings put secure 'myKey' 'myValue';echo x\$?")
    }

    @Test
    fun testSerializeEmptyValue() {
        val bytes = PutSettingRequest(SettingNamespace.SECURE, "myKey", "").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("002Dshell:settings put secure 'myKey' '';echo x\$?")
    }

    @Test
    fun testValidation() {
        assertThat(PutSettingRequest(SettingNamespace.SECURE, "myKey", "myValue").validate().success).isTrue()
        assertThat(PutSettingRequest(SettingNamespace.SECURE, "", "myValue").validate().success).isFalse()
    }
}
