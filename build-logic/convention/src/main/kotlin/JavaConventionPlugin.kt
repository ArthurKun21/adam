import adam.buildlogic.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class JavaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java")
            pluginManager.apply("adam.code.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ProjectConfig.JavaVersion
                targetCompatibility = ProjectConfig.JavaVersion
            }

            tasks.withType<KotlinCompile> {
                compilerOptions {
                    jvmTarget.set(ProjectConfig.JvmTarget)
                }
            }
        }
    }
}
