import java.util.Properties

fun parseVersion(version: String): Triple<Int, Int, Int> {
    val parts = version.split(".")
    require(parts.size == 3) { "Version must be semantic: major.minor.patch" }
    return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

fun bumpVersion(version: String, kind: String): String {
    val (major, minor, patch) = parseVersion(version)
    return when (kind) {
        "major" -> "${major + 1}.0.0"
        "minor" -> "$major.${minor + 1}.0"
        else -> "$major.$minor.${patch + 1}"
    }
}

private val bumpKind = providers
    .gradleProperty("release.bump")
    .orElse("patch")

tasks.register("bumpVersion") {
    group = "release"
    description = "Bumps POM_VERSION in gradle.properties"

    doLast {
        val propsFile = rootProject.file("gradle.properties")
        val props = Properties().apply {
            propsFile.inputStream().use(::load)
        }

        val current = props.getProperty("pom.version")
            ?: error("pom.version is missing in gradle.properties")

        val next = bumpVersion(current, bumpKind.get())
        props.setProperty("pom.version", next)

        propsFile.writer().use { writer ->
            props.store(writer, null)
        }

        println("Version bumped: $current -> $next")
    }
}
