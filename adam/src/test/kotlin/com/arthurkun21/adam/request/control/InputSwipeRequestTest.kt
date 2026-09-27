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

class InputSwipeRequestTest {
    @Test
    fun testSerialize() {
        val bytes = InputSwipeRequest(0, 0, 100, 200, 800).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("002Ashell:input swipe 0 0 100 200 800;echo x$?")
    }

    @Test
    fun testSerializeLongPress() {
        val bytes = InputSwipeRequest.longPress(5, 5, 1000).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0027shell:input swipe 5 5 5 5 1000;echo x$?")
    }

    @Test
    fun testValidation() {
        assertThat(InputSwipeRequest(0, 0, 100, 200).validate().success).isTrue()
        assertThat(InputSwipeRequest(-1, 0, 100, 200).validate().success).isFalse()
        assertThat(InputSwipeRequest(0, 0, 100, 200, 0).validate().success).isFalse()
    }
}
