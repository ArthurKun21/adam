import adam.buildlogic.AdamPublishing
import adam.buildlogic.configureAdamPom

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

plugins {
    id("adam.android.library")
    alias(libs.plugins.vanniktech.maven.publish)
}

mavenPublishing {
    coordinates(AdamPublishing.GROUP, "android-junit4", version.toString())

    pom {
        name.set("android-junit4")
        description.set("Android Debug Bridge helper - JUnit4 Android rules")
        configureAdamPom()
    }
}

android {
    namespace = "com.malinskiy.adam.junit4.android"
}

dependencies {
    api(project(":adam"))
    api(libs.coroutines.core)
    api(project(":android-testrunner-contract"))
}
