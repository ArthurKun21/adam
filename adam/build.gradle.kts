import adam.buildlogic.AdamPublishing
import adam.buildlogic.configureAdamPom
import adam.buildlogic.configureIntegrationTestSourceSet
import adam.buildlogic.configureIntegrationTestTasks
import adam.buildlogic.integrationTestImplementation
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.remove

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
    alias(libs.plugins.protobuf)
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                remove("java")
            }
            it.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
        }
    }
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

dependencies {
    implementation(libs.annotations)
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.coroutines.core)
    implementation(libs.logcat)
    api(libs.protobuf.lite)
    api(libs.grpc.protobuf.lite)
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.okhttp)
    api(libs.grpc.stub)
    implementation(libs.javax.annotations)
    implementation(libs.vertx.core)
    implementation(libs.vertx.kotlin)
    implementation(libs.vertx.coroutines)
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
