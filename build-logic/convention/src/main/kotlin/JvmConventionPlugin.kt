import adam.buildlogic.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class JvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("adam.code.lint")

            extensions.configure<KotlinProjectExtension> {
                explicitApi()
            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ProjectConfig.JavaVersion
                targetCompatibility = ProjectConfig.JavaVersion
            }

            tasks.withType<KotlinCompile> {
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
        }
    }
}
