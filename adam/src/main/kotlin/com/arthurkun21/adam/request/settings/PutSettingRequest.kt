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

import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Writes a settings value via `settings put`. An empty value is allowed (it clears the setting;
 * reads of a missing setting return the literal string `null`)
 */
public class PutSettingRequest(
    namespace: SettingNamespace,
    private val key: String,
    value: String,
) : ShellCommandRequest(
    cmd = "settings put ${namespace.value} '$key' '$value'",
) {
    override fun validate(): ValidationResponse = when {
        key.isBlank() -> ValidationResponse(false, "key must not be blank")
        else -> ValidationResponse.Success
    }
}
