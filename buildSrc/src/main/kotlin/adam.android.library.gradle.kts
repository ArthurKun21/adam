import adam.buildlogic.AndroidConfig
import adam.buildlogic.configureAndroid
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.android.library")
    // id("adam.code.lint")
}

android {
    defaultConfig {
        lint.targetSdk = AndroidConfig.TARGET_SDK
    }
    configureAndroid(this)
}

java {
    sourceCompatibility = AndroidConfig.JavaVersion
    targetCompatibility = AndroidConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    compilerOptions {
        jvmTarget.set(AndroidConfig.JvmTarget)
    }
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.androidx.test.runner)
    api(libs.androidx.test.monitor)
    api(libs.junit4)
}
