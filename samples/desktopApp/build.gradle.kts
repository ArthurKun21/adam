plugins {
    id("adam.compose.desktop")
}

dependencies {
    implementation(project(":adam"))

    implementation(libs.compose.material3)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coil.compose)

    implementation(libs.coroutines.swing)

    implementation(libs.logcat)
}

compose.desktop {
    application {
        mainClass = "com.arthurkun21.adam.samples.desktop.MainKt"

        nativeDistributions {
            packageName = "adam-desktop-sample"
        }
    }
}
