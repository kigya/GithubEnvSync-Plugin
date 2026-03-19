plugins {
    alias(conventions.plugins.versioning)
}

allprojects {
    group = providers.gradleProperty("pom.group").get()
    version = providers.gradleProperty("pom.version").get()
}
