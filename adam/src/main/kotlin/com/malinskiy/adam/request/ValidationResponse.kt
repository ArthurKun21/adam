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

package com.malinskiy.adam.request

import com.malinskiy.adam.Const
import java.io.File
import java.util.*

public data class ValidationResponse(
    public val success: Boolean,
    public val message: String?
) {
    public companion object {
        public val Success: ValidationResponse = ValidationResponse(true, null)

        public fun missingFeature(feature: Feature): String = "${feature.name} is not supported by device"
        public fun missingEitherFeature(vararg feature: Feature): String = "Supported features must include either of ${feature.joinToString()}"
        public fun oneOfFilesShouldBe(extension: String): String = "At least one of the files has to be an ${extension.uppercase(Locale.ENGLISH)} file"
        public fun packageShouldExist(file: File): String = "Package ${file.absolutePath} doesn't exist"
        public fun packageShouldBeRegularFile(file: File): String = "Package ${file.absolutePath} is not a regular file"
        public fun packageShouldBeSupportedExtension(file: File, supported: Set<String>): String =
            "Unsupported package extension ${file.extension}. Should be on of ${supported.joinToString()}}"

        public fun pathShouldNotBeLong(): String = "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes"
    }
}
