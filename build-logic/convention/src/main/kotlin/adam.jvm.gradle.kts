import adam.buildlogic.ProjectConfig
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    kotlin("jvm")
    id("adam.code.lint")
}

kotlin {
    explicitApi()
}

java {
    sourceCompatibility = ProjectConfig.JavaVersion
    targetCompatibility = ProjectConfig.JavaVersion
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(ProjectConfig.JvmTarget)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

pluginManager.withPlugin("jacoco") {
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
        }
    }
}
