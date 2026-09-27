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

/**
 * Contract between the host and the on-device companion helper (`:companion` module).
 * The helper answers on loopback [DEVICE_PORT] after the host deploys it via
 * [CompanionManager]
 */
public object CompanionConstants {
    public const val APPLICATION_ID: String = "com.arthurkun21.adam.companion"
    public const val SERVICE_COMPONENT: String = "$APPLICATION_ID/.CompanionAccessibilityService"
    public const val RECEIVER_COMPONENT: String = "$APPLICATION_ID/.TokenReceiver"
    public const val ACTION_SET_TOKEN: String = "$APPLICATION_ID.SET_TOKEN"
    public const val EXTRA_TOKEN: String = "token"
    public const val DEVICE_PORT: Int = 18888

    /**
     * Minimum protocol_version the host accepts in a `/ping` response. Bumped whenever the
     * helper's HTTP contract changes in a way an older deployed APK cannot serve
     */
    public const val PROTOCOL_VERSION: Int = 2
}
