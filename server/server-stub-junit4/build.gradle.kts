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
    id("adam.jvm")
    id("jacoco")
    alias(libs.plugins.vanniktech.maven.publish)
}

mavenPublishing {
    coordinates(AdamPublishing.GROUP, "server-stub-junit4", version.toString())

    pom {
        name.set("server-stub-junit4")
        description.set("Android Debug Bridge helper - Server stub JUnit4")
        configureAdamPom()
    }
}

dependencies {
    api(project(":server:server-stub"))
    implementation(libs.junit4)
    implementation(libs.coroutines.core)

    testImplementation(libs.coroutines.debug)
}
