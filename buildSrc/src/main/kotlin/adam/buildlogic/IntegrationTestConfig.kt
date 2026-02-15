package adam.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

private const val INTEGRATION_TEST = "integrationTest"
private const val INTEGRATION_TEST_IMPLEMENTATION = "integrationTestImplementation"
private const val INTEGRATION_TEST_RUNTIME_ONLY = "integrationTestRuntimeOnly"
private const val JACOCO_INTEGRATION_TEST_REPORT = "jacocoIntegrationTestReport"

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

data class IntegrationTestTasks(
    val integrationTest: TaskProvider<Test>,
    val jacocoIntegrationTestReport: TaskProvider<JacocoReport>,
)

fun Project.configureIntegrationTestTasks(
    configureIntegrationTest: Test.(TaskProvider<JacocoReport>) -> Unit = {},
    configureJacocoReport: JacocoReport.(TaskProvider<Test>) -> Unit = {},
): IntegrationTestTasks {
    val sourceSets = extensions.getByType(SourceSetContainer::class.java)

    val integrationTest = tasks.register<Test>(INTEGRATION_TEST) {
        description = "Runs integration tests"
        group = "verification"

        testClassesDirs = sourceSets.getByName(INTEGRATION_TEST).output.classesDirs
        classpath = sourceSets.getByName(INTEGRATION_TEST).runtimeClasspath
        shouldRunAfter("test")

        extensions.configure(JacocoTaskExtension::class.java) {
            includes = listOf("**")
        }
    }

    val jacocoIntegrationTestReport = tasks.register<JacocoReport>(JACOCO_INTEGRATION_TEST_REPORT) {
        description = "Generates code coverage report for integrationTest task"
        group = "verification"

        sourceSets(sourceSets.getByName(INTEGRATION_TEST))
        classDirectories.setFrom(sourceSets.getByName("main").output.classesDirs)
    }

    integrationTest.configure {
        outputs.upToDateWhen { false }
        configureIntegrationTest(jacocoIntegrationTestReport)
    }

    jacocoIntegrationTestReport.configure {
        configureJacocoReport(integrationTest)
    }

    tasks.named("check") {
        dependsOn(integrationTest, jacocoIntegrationTestReport)
    }

    return IntegrationTestTasks(integrationTest, jacocoIntegrationTestReport)
}

private fun ConfigurationContainer.configureIntegrationTestConfigurations() {
    getByName(INTEGRATION_TEST_IMPLEMENTATION).extendsFrom(getByName("implementation"))
    getByName(INTEGRATION_TEST_RUNTIME_ONLY).extendsFrom(getByName("runtimeOnly"))
}
