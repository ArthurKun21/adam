package adam.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName

private const val INTEGRATION_TEST = "integrationTest"
private const val INTEGRATION_TEST_IMPLEMENTATION = "integrationTestImplementation"
private const val INTEGRATION_TEST_RUNTIME_ONLY = "integrationTestRuntimeOnly"

fun Project.configureIntegrationTestSourceSet() {
    extensions.configure<SourceSetContainer> {
        create(INTEGRATION_TEST) {
            compileClasspath += getByName("main").output
            runtimeClasspath += getByName("main").output
        }
    }

    configurations.configureIntegrationTestConfigurations()
}

fun DependencyHandler.integrationTestImplementation(dependencyNotation: Any): Dependency? =
    add(INTEGRATION_TEST_IMPLEMENTATION, dependencyNotation)

private fun ConfigurationContainer.configureIntegrationTestConfigurations() {
    getByName(INTEGRATION_TEST_IMPLEMENTATION).extendsFrom(getByName("implementation"))
    getByName(INTEGRATION_TEST_RUNTIME_ONLY).extendsFrom(getByName("runtimeOnly"))
}
