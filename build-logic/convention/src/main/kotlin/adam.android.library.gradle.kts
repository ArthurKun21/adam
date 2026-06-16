import adam.buildlogic.ProjectConfig
import adam.buildlogic.configureAndroid
import adam.buildlogic.library
import adam.buildlogic.libs

plugins {
    id("com.android.library")
    id("adam.code.lint")
}

kotlin {
    explicitApi()
}

android {
    defaultConfig {
        lint.targetSdk = ProjectConfig.TARGET_SDK
    }
    configureAndroid(this)
}

java {
    sourceCompatibility = ProjectConfig.JavaVersion
    targetCompatibility = ProjectConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(ProjectConfig.JvmTarget)
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.library("androidx-test-runner"))
    api(libs.library("androidx-test-monitor"))
    api(libs.library("junit4"))
}
