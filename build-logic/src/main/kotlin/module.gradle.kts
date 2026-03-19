import config.DetektConfigs
import ext.libs
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.kotlin.dsl.withType

plugins {
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    config.from(rootProject.file(DetektConfigs.MAIN))
    autoCorrect = System.getProperty("DETEKT_AUTOCORRECT")?.toBooleanStrictOrNull() ?: true
    parallel = true
    allRules = false
    debug = false

    source.from(
        "src/main/kotlin",
        "src/main/java",
    )
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

private val githubUserProvider = providers
    .gradleProperty("gpr.user")
    .orElse(providers.environmentVariable("GITHUB_USERNAME"))

private val githubTokenProvider = providers
    .gradleProperty("gpr.key")
    .orElse(providers.environmentVariable("GITHUB_TOKEN"))

publishing {
    repositories {
        if (githubUserProvider.isPresent && githubTokenProvider.isPresent) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/BashlikovV/StringIndexer")

                credentials {
                    username = githubUserProvider.get()
                    password = githubTokenProvider.get()
                }
            }
        }
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
