package adam.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

val Project.libs get() = the<LibrariesForLibs>()

internal fun configureAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = AndroidConfig.COMPILE_SDK

        defaultConfig.apply {
            minSdk = AndroidConfig.MIN_SDK
        }

        compileOptions.apply {
            sourceCompatibility = AndroidConfig.JavaVersion
            targetCompatibility = AndroidConfig.JavaVersion
        }
    }
}