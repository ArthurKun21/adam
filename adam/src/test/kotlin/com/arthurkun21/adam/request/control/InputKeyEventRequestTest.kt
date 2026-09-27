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
import com.malinskiy.adam.Const
import org.junit.Test

class InputKeyEventRequestTest {
    @Test
    fun testSerialize() {
        val bytes = InputKeyEventRequest(66).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0020shell:input keyevent 66;echo x$?")
    }

    @Test
    fun testSerializeMetastate() {
        val bytes = InputKeyEventRequest(66, metastate = 1).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0029shell:input keyevent --meta 1 66;echo x$?")
    }

    @Test
    fun testSerializeLongpress() {
        val bytes = InputKeyEventRequest(66, longpress = true).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("002Cshell:input keyevent --longpress 66;echo x$?")
    }

    @Test
    fun testSerializeKeyCodeEnum() {
        val bytes = InputKeyEventRequest(AndroidKeyCode.ENTER).serialize()
        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0020shell:input keyevent 66;echo x$?")
    }
}
