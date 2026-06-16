import adam.buildlogic.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class ComposeDesktopConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")
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

            val composeDependencies = extensions.getByType(ComposeExtension::class.java).dependencies

            dependencies {
                add("implementation", composeDependencies.desktop.currentOs)
            }

            extensions.configure<ComposeExtension> {
                (this as ExtensionAware).extensions.configure<DesktopExtension> {
                    application {
                        nativeDistributions {
                            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                            packageVersion = "1.0.0"
                        }
                    }
                }
            }
        }
    }
}
