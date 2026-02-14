import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.publish.PublishingExtension
import java.util.*

plugins {
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

version = providers.environmentVariable("RELEASE_TAG")
    .map { it.removePrefix("v") }
    .getOrElse(libs.versions.adam.get())

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase(Locale.ENGLISH).contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

subprojects {
    version = rootProject.version

    plugins.withId("com.vanniktech.maven.publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/ArthurKun21/adam")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
