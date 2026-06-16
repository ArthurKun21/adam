import adam.buildlogic.libs
import adam.buildlogic.version

plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.version("ktlint-core")).editorConfigOverride(
            mapOf("ktlint_standard_annotation" to "disabled"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("xml") {
        target("**/*.xml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
