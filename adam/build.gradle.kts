import adam.buildlogic.AdamPublishing
import adam.buildlogic.configureAdamPom
import adam.buildlogic.configureIntegrationTestSourceSet
import adam.buildlogic.configureIntegrationTestTasks
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
    id("org.jetbrains.dokka")
    alias(libs.plugins.kotlinx.rpc)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.maven.publish)
    id("idea")
}

mavenPublishing {
    coordinates(AdamPublishing.GROUP, "adam", version.toString())

    pom {
        name.set("adam")
        description.set("Android Debug Bridge helper - core library")
        configureAdamPom()
    }
}

rpc {
    protoc()
}

configureIntegrationTestSourceSet()

val integrationTestTasks = configureIntegrationTestTasks(
    configureIntegrationTest = { jacocoIntegrationTestReport ->
        finalizedBy(jacocoIntegrationTestReport)
    },
    configureJacocoReport = { integrationTest ->
        executionData(fileTree(layout.buildDirectory).include("jacoco/integrationTest.exec"))
        mustRunAfter(integrationTest)
    },
)

val integrationTest = integrationTestTasks.integrationTest

val jacocoIntegrationTestReport = integrationTestTasks.jacocoIntegrationTestReport
jacocoIntegrationTestReport.configure {
    reports {
        xml.required.set(true)
    }
}

// See https://github.com/jacoco/jacoco/issues/1357
tasks.withType<Test> {
    extensions.configure<JacocoTaskExtension> {
        includes = listOf("com.malinskiy.adam.*")
    }
}

val connectedAndroidTest = tasks.register<Test>("connectedAndroidTest") {
    description = "Runs integration tests"
    group = "verification"

    dependsOn(integrationTest)
}

val jacocoCombinedTestReport = tasks.register<JacocoReport>("jacocoCombinedTestReport") {
    description = "Generates code coverage report for all test tasks"
    group = "verification"

    executionData(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
    sourceSets(sourceSets.getByName("integrationTest"), sourceSets.getByName("test"))
    classDirectories.setFrom(sourceSets.getByName("main").output.classesDirs)
    mustRunAfter(tasks["test"], integrationTest)
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.rootDir.resolve("docs/api"))
    }
}

// kotlinx-rpc generates the messages file and the service file with identical relative paths
// (com/android/emulator/control/EmulatorController.kt) in two source roots, which collides inside
// the sources jar. Generated code is reproducible via `rpc { protoc() }`, so ship hand-written
// sources only.
tasks.withType<Jar>().configureEach {
    if (name == "sourcesJar") {
        exclude { element -> element.file.absolutePath.contains("protoBuild") }
    }
}

dependencies {
    implementation(libs.annotations)
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.coroutines.core)
    implementation(libs.logcat)
    // kotlinx-rpc-protobuf-lite-jvm declares an unused runtime dependency on protobuf-javalite
    // (no class in the module references com.google.protobuf); keep protobuf-java out of the tree
    api(libs.kotlinx.rpc.protobuf) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    api(libs.kotlinx.rpc.grpc.core)
    api(libs.kotlinx.rpc.grpc.client)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.grpc.okhttp)
    implementation(libs.ktor.network)
    implementation(libs.apache.commons.pool2)

    testImplementation(libs.assertk)
    testImplementation(libs.junit4)
    testImplementation(libs.image.comparison)
    testImplementation(kotlin("reflect"))
    testImplementation(libs.coroutines.debug)
    testImplementation(project(":server:server-stub-junit4"))

    integrationTestImplementation(libs.coroutines.debug)
    integrationTestImplementation(libs.assertk)
    integrationTestImplementation(libs.junit4)
    integrationTestImplementation(kotlin("reflect"))
}
