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
    coordinates("com.github.ArthurKun21", "server-stub", version.toString())

    pom {
        name.set("server-stub")
        description.set("Android Debug Bridge helper - Server stub")
        url.set("https://github.com/ArthurKun21/adam")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ArthurKun21")
                name.set("Arthur")
                email.set("16458204+ArthurKun21@users.noreply.github.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/ArthurKun21/adam.git")
            developerConnection.set("scm:git:ssh://github.com/ArthurKun21/adam.git")
            url.set("https://github.com/ArthurKun21/adam")
        }
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

fun DependencyHandler.`integrationTestImplementation`(dependencyNotation: Any): Dependency? =
    add("integrationTestImplementation", dependencyNotation)


val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    jacoco {
        include("**")
    }
}
integrationTest.configure {
    outputs.upToDateWhen { false }
}

val jacocoIntegrationTestReport = tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    description = "Generates code coverage report for integrationTest task"
    group = "verification"
    reports {
        xml.required.set(true)
    }

    executionData(integrationTest)
    sourceSets(sourceSets.getByName("integrationTest"))
    classDirectories.setFrom(sourceSets.getByName("main").output.classesDirs)
}
tasks.check { dependsOn(integrationTest, jacocoIntegrationTestReport) }

tasks.jacocoTestReport {
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
