package adam.buildlogic

import org.gradle.api.publish.maven.MavenPom

object AdamPublishing {
    const val GROUP = "com.github.ArthurKun21"
    const val REPOSITORY_URL = "https://github.com/ArthurKun21/adam"
}

fun MavenPom.configureAdamPom() {
    url.set(AdamPublishing.REPOSITORY_URL)
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    developers {
        developer {
            id.set("ArthurKun21")
            name.set("Arthur")
            email.set("16458204+ArthurKun21@users.noreply.github.com")
        }
    }
    scm {
        connection.set("scm:git:git://github.com/ArthurKun21/adam.git")
        developerConnection.set("scm:git:ssh://github.com/ArthurKun21/adam.git")
        url.set(AdamPublishing.REPOSITORY_URL)
    }
}
