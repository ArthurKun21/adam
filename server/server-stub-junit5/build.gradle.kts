import org.jetbrains.kotlin.gradle.dsl.JvmTarget as KotlinJvmTarget

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
    kotlin("jvm")
    id("jacoco")
}

Deployment.initialize(project)

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = KotlinJvmTarget.JVM_11
    kotlinOptions.apiVersion = "1.5"
    kotlinOptions.languageVersion = KotlinJvmTarget.JVM_11
}

dependencies {
    api(project(":server:server-stub"))
    implementation(TestLibraries.junit5)
    implementation(TestLibraries.junit5commons)
    implementation(kotlin("reflect", version = Versions.kotlin))
    implementation(Libraries.coroutines)

    testImplementation(TestLibraries.coroutinesDebug)
}
