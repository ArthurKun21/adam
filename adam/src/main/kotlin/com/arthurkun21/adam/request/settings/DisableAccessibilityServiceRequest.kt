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

import com.arthurkun21.adam.request.settings.SettingNamespace.SECURE
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.ValidationResponse

/**
 * Removes an accessibility service from the `enabled_accessibility_services` secure setting. When
 * the last service is removed, the `accessibility_enabled` flag is turned off as well
 *
 * @param serviceComponent component in the `pkg/.Service` or `pkg/fully.qualified.Service` form
 */
public class DisableAccessibilityServiceRequest(
    private val serviceComponent: String,
) : MultiRequest<Unit>() {
    override suspend fun execute(
        androidDebugBridgeClient: AndroidDebugBridgeClient,
        serial: String?,
    ) {
        with(androidDebugBridgeClient) {
            val current = execute(GetSettingRequest(SECURE, KEY_ENABLED_ACCESSIBILITY_SERVICES), serial)
            val services = current
                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
                ?.split(':')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
                .filterNot { it.equals(serviceComponent, ignoreCase = true) }

            execute(
                PutSettingRequest(SECURE, KEY_ENABLED_ACCESSIBILITY_SERVICES, services.joinToString(":")),
                serial,
            )

            if (services.isEmpty()) {
                execute(PutSettingRequest(SECURE, KEY_ACCESSIBILITY_ENABLED, "0"), serial)
            }
        }
    }

    override fun validate(): ValidationResponse = when {
        serviceComponent.isBlank() -> ValidationResponse(false, "serviceComponent must not be blank")
        else -> ValidationResponse.Success
    }

    public companion object {
        public const val KEY_ENABLED_ACCESSIBILITY_SERVICES: String =
            EnableAccessibilityServiceRequest.KEY_ENABLED_ACCESSIBILITY_SERVICES
        public const val KEY_ACCESSIBILITY_ENABLED: String =
            EnableAccessibilityServiceRequest.KEY_ACCESSIBILITY_ENABLED
    }
}
