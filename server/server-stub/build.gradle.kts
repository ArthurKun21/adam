import adam.buildlogic.AdamPublishing
import adam.buildlogic.configureIntegrationTestSourceSet
import adam.buildlogic.configureIntegrationTestTasks
import adam.buildlogic.configureAdamPom
import adam.buildlogic.integrationTestImplementation

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
    coordinates(AdamPublishing.GROUP, "server-stub", version.toString())

    pom {
        name.set("server-stub")
        description.set("Android Debug Bridge helper - Server stub")
        configureAdamPom()
    }
}

configureIntegrationTestSourceSet()

val integrationTestTasks = configureIntegrationTestTasks(
    configureJacocoReport = { integrationTest ->
        executionData(integrationTest)
    },
)

val jacocoIntegrationTestReport = integrationTestTasks.jacocoIntegrationTestReport
jacocoIntegrationTestReport.configure {
    reports {
        xml.required.set(true)
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.coroutines.core)
    api(project(":adam"))
    api(libs.ktor.network)
    api(libs.assertk)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.debug)

    integrationTestImplementation(libs.junit4)
    integrationTestImplementation(libs.coroutines.debug)
}
