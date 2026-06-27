plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.compose.compiler.gradle)
    implementation(libs.compose.gradle)
    implementation(libs.androidx.gradle)
    implementation(libs.spotless.gradle)
}

gradlePlugin {
    plugins {
        register("adamAndroidLibrary") {
            id = "adam.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("adamCodeLint") {
            id = "adam.code.lint"
            implementationClass = "SpotlessConventionPlugin"
        }
        register("adamJava") {
            id = "adam.java"
            implementationClass = "JavaConventionPlugin"
        }
        register("adamJvm") {
            id = "adam.jvm"
            implementationClass = "JvmConventionPlugin"
        }
        register("adamComposeDesktop") {
            id = "adam.compose.desktop"
            implementationClass = "ComposeDesktopConventionPlugin"
        }
    }
}
