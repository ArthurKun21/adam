import adam.buildlogic.ProjectConfig
import adam.buildlogic.configureAndroid
import adam.buildlogic.library
import adam.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("adam.code.lint")

            extensions.configure<KotlinProjectExtension> {
                explicitApi()
            }

            extensions.configure<LibraryExtension> {
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
                add("api", libs.library("androidx-test-runner"))
                add("api", libs.library("androidx-test-monitor"))
                add("api", libs.library("junit4"))
            }
        }
    }
}
