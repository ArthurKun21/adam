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
 * Enables an accessibility service without user interaction by appending it to the
 * `enabled_accessibility_services` secure setting and turning the `accessibility_enabled` flag on.
 *
 * This is how the adb shell user deploys companion services (e.g. the `:companion` module) on
 * non-rooted devices. Android binds the service once it is enabled; a dead-but-enabled service is
 * revived by disabling and re-enabling it
 *
 * @param serviceComponent component in the `pkg/.Service` or `pkg/fully.qualified.Service` form
 */
public class EnableAccessibilityServiceRequest(
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

            if (services.none { it.equals(serviceComponent, ignoreCase = true) }) {
                val updated = (services + serviceComponent).joinToString(":")
                execute(PutSettingRequest(SECURE, KEY_ENABLED_ACCESSIBILITY_SERVICES, updated), serial)
            }
            execute(PutSettingRequest(SECURE, KEY_ACCESSIBILITY_ENABLED, "1"), serial)
        }
    }

    override fun validate(): ValidationResponse = when {
        serviceComponent.isBlank() -> ValidationResponse(false, "serviceComponent must not be blank")
        else -> ValidationResponse.Success
    }

    public companion object {
        public const val KEY_ENABLED_ACCESSIBILITY_SERVICES: String = "enabled_accessibility_services"
        public const val KEY_ACCESSIBILITY_ENABLED: String = "accessibility_enabled"
    }
}
