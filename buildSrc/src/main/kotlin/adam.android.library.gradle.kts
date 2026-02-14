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

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("src/androidTest/kotlin")
        }
    }
}

java {
    sourceCompatibility = AndroidConfig.JavaVersion
    targetCompatibility = AndroidConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.androidx.test.runner)
    api(libs.androidx.test.monitor)
    api(libs.junit4)
}
