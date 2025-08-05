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
