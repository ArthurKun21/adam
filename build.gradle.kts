import org.gradle.api.publish.PublishingExtension
import java.util.Locale

plugins {
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

version = providers.environmentVariable("RELEASE_TAG")
    .map { it.removePrefix("v") }
    .getOrElse(libs.versions.adam.get())

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
