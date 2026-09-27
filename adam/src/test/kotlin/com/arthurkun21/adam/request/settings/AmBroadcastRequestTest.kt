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

class AmBroadcastRequestTest {
    @Test
    fun testSerializeAction() {
        val bytes = AmBroadcastRequest(action = "ADB_INPUT_B64", stringExtras = mapOf("msg" to "aGVsbG8=")).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0042shell:am broadcast -a ADB_INPUT_B64 --es 'msg' 'aGVsbG8=';echo x\$?")
    }

    @Test
    fun testSerializeComponent() {
        val bytes = AmBroadcastRequest(
            action = "com.example.ACTION",
            component = "com.example/.Receiver",
            stringExtras = mapOf("msg" to "hello"),
        ).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo(
                "005Dshell:am broadcast -a com.example.ACTION -n com.example/.Receiver " +
                    "--es 'msg' 'hello';echo x\$?",
            )
    }

    @Test
    fun testValidation() {
        assertThat(AmBroadcastRequest(action = "com.example.ACTION").validate().success).isTrue()
        assertThat(AmBroadcastRequest(component = "com.example/.Receiver").validate().success).isTrue()
        assertThat(AmBroadcastRequest().validate().success).isFalse()
    }
}
