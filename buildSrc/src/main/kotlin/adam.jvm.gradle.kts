import adam.buildlogic.AndroidConfig

plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = AndroidConfig.JavaVersion
    targetCompatibility = AndroidConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    compilerOptions {
        jvmTarget.set(AndroidConfig.JvmTarget)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}
