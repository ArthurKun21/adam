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

class InputTapRequestTest {
    @Test
    fun testSerialize() {
        val bytes = InputTapRequest(1, 2).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("001Cshell:input tap 1 2;echo x$?")
    }

    @Test
    fun testSerializeWithDuration() {
        val bytes = InputTapRequest(1, 2, 1000).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0027shell:input swipe 1 2 1 2 1000;echo x$?")
    }

    @Test
    fun testValidation() {
        assertThat(InputTapRequest(1, 2).validate().success).isTrue()
        assertThat(InputTapRequest(1, 2, 500).validate().success).isTrue()
        assertThat(InputTapRequest(-1, 2).validate().success).isFalse()
        assertThat(InputTapRequest(1, -2).validate().success).isFalse()
        assertThat(InputTapRequest(1, 2, 0).validate().success).isFalse()
    }
}
