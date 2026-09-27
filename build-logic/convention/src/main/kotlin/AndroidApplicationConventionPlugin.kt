import adam.buildlogic.ProjectConfig
import adam.buildlogic.configureAndroid
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Android application convention for the on-device companion helper APK
 */
@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("adam.code.lint")

            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    lint.targetSdk = ProjectConfig.TARGET_SDK
                }
                configureAndroid(this)
            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ProjectConfig.JavaVersion
                targetCompatibility = ProjectConfig.JavaVersion
            }

            tasks.withType<KotlinCompile> {
                compilerOptions {
                    jvmTarget.set(ProjectConfig.JvmTarget)
                }
            }

            dependencies {
                add("implementation", kotlin("stdlib-jdk8"))
            }
        }
    }
}
