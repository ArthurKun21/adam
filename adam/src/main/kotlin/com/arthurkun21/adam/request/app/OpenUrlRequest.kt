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

import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

/**
 * Opens a URL on the device via the VIEW intent
 */
public class OpenUrlRequest(private val url: String) : ShellCommandRequest(
    cmd = "am start -a android.intent.action.VIEW -d '$url'",
) {
    override fun validate(): ValidationResponse = when {
        url.isBlank() -> ValidationResponse(false, "url must not be blank")
        else -> ValidationResponse.Success
    }
}
