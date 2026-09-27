/*
 * Copyright 2026 Google LLC
 * Portions Copyright (C) 2026 ArthurKun21
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

// Ported to Kotlin from google/artemis packages/artemis-accessibility-helper (Apache 2.0)

package com.arthurkun21.adam.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives the host's session token.
 *
 * The receiver is guarded in the manifest by
 * `android.permission.WRITE_SECURE_SETTINGS`, a permission ordinary apps
 * cannot hold but the adb shell user does, so only something with adb access to
 * the device can set (or clear) the token:
 *
 * ```
 * adb shell am broadcast -n com.arthurkun21.adam.companion/.TokenReceiver \
 *     -a com.arthurkun21.adam.companion.SET_TOKEN --es token <hex>
 * ```
 */
class TokenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action != ACTION_SET_TOKEN) {
            return
        }
        TokenStore.set(intent.getStringExtra(EXTRA_TOKEN))
        Log.i(TAG, if (TokenStore.isSet()) "Session token set" else "Session token cleared")
    }

    companion object {
        private const val TAG = "AdamCompanionToken"
        const val ACTION_SET_TOKEN = "com.arthurkun21.adam.companion.SET_TOKEN"
        const val EXTRA_TOKEN = "token"
    }
}
