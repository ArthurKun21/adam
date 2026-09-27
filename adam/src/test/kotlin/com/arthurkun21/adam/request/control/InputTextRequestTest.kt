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

package com.arthurkun21.adam.request.control

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.Const
import org.junit.Test

class InputTextRequestTest {
    @Test
    fun testSerialize() {
        val bytes = InputTextRequest("hello world").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0026shell:input text hello%sworld;echo x$?")
    }

    @Test
    fun testSerializeMultiline() {
        val bytes = InputTextRequest("hi\nthere").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0045shell:input text hi && input keyevent 66 && input text there;echo x$?")
    }

    @Test
    fun testSerializeEscaping() {
        val bytes = InputTextRequest("\$x & y").serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0024shell:input text \\\$x%s\\&%sy;echo x$?")
    }

    @Test
    fun testEscapeText() {
        assertThat(InputTextRequest.escapeText("a b")).isEqualTo("a%sb")
        assertThat(InputTextRequest.escapeText("a'b\"c\\d\$e")).isEqualTo("a\\'b\\\"c\\\\d\\\$e")
    }

    @Test
    fun testValidation() {
        assertThat(InputTextRequest("hello").validate().success).isTrue()
        assertThat(InputTextRequest("").validate().success).isFalse()
    }
}
