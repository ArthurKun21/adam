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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * The session token the host pushed for this process lifetime.
 *
 * The loopback server is reachable by every app on the device, so requests are
 * only served when they carry this token. It lives in memory only: a killed or
 * re-bound service starts without one and answers 401 until the host pushes it
 * again (which the host does on every attach and on every 401).
 */
object TokenStore {

    @Volatile
    private var token: String? = null

    fun set(value: String?) {
        token = if (value.isNullOrEmpty()) null else value
    }

    fun isSet(): Boolean = token != null

    /** Constant-time comparison so a local app cannot guess the token byte by byte. */
    fun matches(candidate: String?): Boolean {
        val current = token
        if (current == null || candidate == null) {
            return false
        }
        return MessageDigest.isEqual(
            current.toByteArray(StandardCharsets.UTF_8),
            candidate.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
