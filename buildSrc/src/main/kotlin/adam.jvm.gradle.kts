import adam.buildlogic.ProjectConfig

plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = ProjectConfig.JavaVersion
    targetCompatibility = ProjectConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>{
    compilerOptions {
        jvmTarget.set(ProjectConfig.JvmTarget)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}
