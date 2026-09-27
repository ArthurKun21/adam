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

plugins {
    id("adam.android.application")
}

// keep targetSdk in sync with adam.buildlogic.ProjectConfig.TARGET_SDK
android {
    namespace = "com.arthurkun21.adam.companion"

    defaultConfig {
        applicationId = "com.arthurkun21.adam.companion"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
