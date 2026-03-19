plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.gradleNexus.publish.plugin)
    implementation(libs.detekt.gradle.plugin)
    libs {
        implementation(gradleApi())
        api(files(javaClass.superclass.protectionDomain.codeSource.location))
    }
}
