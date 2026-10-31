plugins {
    // buildSrc is a separate build, so it needs its own toolchain resolver to download JDK 25.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {

    // Use Maven Central and the Gradle Plugin Portal for resolving dependencies in the shared build logic (`buildSrc`) project.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }

    // Reuse the version catalog from the main build.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"