package ext

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.accessors.dm.LibrariesForLibs

internal inline val Project.libs: LibrariesForLibs
    get() = (this as ExtensionAware)
        .extensions
        .getByName("libs") as LibrariesForLibs
