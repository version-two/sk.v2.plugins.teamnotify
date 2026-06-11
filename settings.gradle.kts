rootProject.name = "team-notify"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/teamcity/maven/")
        }
    }
}

// Lets Gradle auto-provision the JDK 21 toolchain required to build against TeamCity 2026.1
// (whose API is compiled to Java 21 bytecode) on machines that don't have JDK 21 installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
