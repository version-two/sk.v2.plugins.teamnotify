import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
 

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.github.rodm.teamcity-server") version "1.5.2"
}

group = "sk.v2.plugins.teamnotify"

// Auto-increment build number and compose semantic version + build metadata
val buildNumberFile = file("build.number")
val buildNumber = if (buildNumberFile.exists()) {
    val currentNumber = buildNumberFile.readText().trim().toIntOrNull() ?: 0
    val newNumber = currentNumber + 1
    buildNumberFile.writeText(newNumber.toString())
    newNumber
} else {
    buildNumberFile.writeText("1")
    1
}

version = "1.2.0+$buildNumber-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/teamcity/teamcity-api")
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    implementation(kotlin("stdlib-jdk8"))
    api("org.jetbrains.teamcity:server-api:2025.07") {
        exclude(group = "org.springframework")
    }
    api("org.jetbrains.teamcity:common-api:2025.07") {
        exclude(group = "org.springframework")
    }
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.test {
    useJUnitPlatform()
}

teamcity {
    server {
        descriptor = file("src/main/resources/META-INF/teamcity-plugin.xml")
        tokens = mapOf("Version" to project.version)
    }
}

// Packaging is SDK-compliant: teamcity-plugin.xml at ZIP root; Spring bean definition file
// META-INF/build-server-plugin-<plugin>.xml inside the server JAR; web resources in buildServerResources/
