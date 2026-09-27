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

package com.arthurkun21.adam.companion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of the helper's `/ping` endpoint */
@Serializable
public data class CompanionPing(
    public val success: Boolean = false,
    @SerialName("version_code") public val versionCode: Long = -1,
    @SerialName("version_name") public val versionName: String? = null,
    @SerialName("protocol_version") public val protocolVersion: Int = 0,
    public val port: Int = 0,
    @SerialName("token_set") public val tokenSet: Boolean = false,
    public val authenticated: Boolean = false,
    @SerialName("package") public val packageName: String? = null,
    public val activity: String? = null,
    public val error: String? = null,
)

/**
 * Response of the helper's `/snapshot` endpoint: hierarchy XML and, on API 30+, a JPEG
 * screenshot captured atomically with the hierarchy
 */
@Serializable
public data class CompanionSnapshot(
    public val success: Boolean = false,
    public val rotation: Int = 0,
    public val width: Int = 0,
    public val height: Int = 0,
    public val xml: String = "",
    @SerialName("screenshot_base64") public val screenshotBase64: String? = null,
    @SerialName("has_screenshot") public val hasScreenshot: Boolean = false,
    @SerialName("screenshot_error") public val screenshotError: String? = null,
    @SerialName("node_count") public val nodeCount: Int = 0,
    @SerialName("package") public val packageName: String? = null,
    public val activity: String? = null,
    public val error: String? = null,
)

/** Response of the helper's `/action` endpoint */
@Serializable
public data class CompanionActionResult(
    public val success: Boolean = false,
    public val error: String? = null,
)
