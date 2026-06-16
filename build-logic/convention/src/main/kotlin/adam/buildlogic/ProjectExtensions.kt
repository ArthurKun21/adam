package adam.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

fun VersionCatalog.version(alias: String) = findVersion(alias).get().requiredVersion

internal fun configureAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK

        defaultConfig.apply {
            minSdk = ProjectConfig.MIN_SDK
        }

        compileOptions.apply {
            sourceCompatibility = ProjectConfig.JavaVersion
            targetCompatibility = ProjectConfig.JavaVersion
        }
    }
}
