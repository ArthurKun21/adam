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

/**
 * Common Android keycodes as accepted by `input keyevent`
 *
 * Values mirror android.view.KeyEvent
 */
public enum class AndroidKeyCode(public val code: Int) {
    HOME(3),
    BACK(4),
    MENU(82),
    APP_SWITCH(187),
    POWER(26),
    VOLUME_UP(24),
    VOLUME_DOWN(25),
    DPAD_UP(19),
    DPAD_DOWN(20),
    DPAD_LEFT(21),
    DPAD_RIGHT(22),
    DPAD_CENTER(23),
    TAB(61),
    SPACE(62),
    ENTER(66),
    DEL(67),
    FORWARD_DEL(112),
    ESCAPE(111),
    MOVE_HOME(122),
    MOVE_END(123),
    PAGE_UP(92),
    PAGE_DOWN(93),
    CUT(277),
    COPY(278),
    PASTE(279),
    SEARCH(84),
    NOTIFICATION(83),
}
