import adam.buildlogic.ProjectConfig

plugins {
    id("java")
    id("adam.code.lint")
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
