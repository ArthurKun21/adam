import adam.buildlogic.ProjectConfig
import adam.buildlogic.configureAndroid
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.android.library")
    // id("adam.code.lint")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    compilerOptions {
        jvmTarget.set(ProjectConfig.JvmTarget)
    }
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.androidx.test.runner)
    api(libs.androidx.test.monitor)
    api(libs.junit4)
}
