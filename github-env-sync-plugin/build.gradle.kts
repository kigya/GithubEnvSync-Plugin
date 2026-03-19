plugins {
    kotlin("jvm")
    alias(conventions.plugins.module)
    id("java-gradle-plugin")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.jackson.module.kotlin)
}

gradlePlugin {
    plugins {
        create("githubEnvSyncPlugin") {
            id = "dev.kigya.github-env-sync"
            implementationClass = "GithubEnvSyncPlugin"
            displayName = "GitHub Env Sync Plugin"
            description = "Authorize via GitHub, verify repo access, fetch Actions Variables, and generate env files"
        }
    }
}
