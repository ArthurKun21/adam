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

import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Injects a key event
 *
 * @param keycode keycode as accepted by `input keyevent`, see [AndroidKeyCode] for common values
 * @param metastate optional meta state bitmask (e.g. 1 for shift) injected with the event.
 * Uses the `--meta` flag of the `input` command
 * @param longpress when true, injects the event with the `--longpress` flag
 */
public class InputKeyEventRequest(
    keycode: Int,
    private val metastate: Int? = null,
    private val longpress: Boolean = false,
) : ShellCommandRequest(
    cmd = StringBuilder("input keyevent").apply {
        if (longpress) append(" --longpress")
        if (metastate != null) append(" --meta $metastate")
        append(" $keycode")
    }.toString(),
) {
    public constructor(
        keycode: AndroidKeyCode,
        metastate: Int? = null,
        longpress: Boolean = false,
    ) : this(keycode.code, metastate, longpress)
}
