plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.androidx.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.spotless.gradle)

    // workaround to enable version catalogs (libs) in buildSrc
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
