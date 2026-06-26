import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.github.rodm.teamcity-server") version "1.5.2"
    id("org.jetbrains.changelog") version "2.2.1"
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

// Check if this is a release build (use -Prelease flag)
val isRelease = project.hasProperty("release")
val baseVersion = "1.4.0"
version = if (isRelease) {
    "$baseVersion+$buildNumber"
} else {
    "$baseVersion+$buildNumber-SNAPSHOT"
}

repositories {
    mavenCentral()
    // Primary mirror for the TeamCity Open API artifacts (serves 2026.1; the Space repo
    // below is kept as a fallback and is occasionally unavailable).
    maven {
        url = uri("https://download.jetbrains.com/teamcity-repository")
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/teamcity/teamcity-api")
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    // The test suite mixes JUnit 4 (payloads/, validation/ use org.junit.Test) and
    // JUnit 5 (services/ use org.junit.jupiter). Both APIs are needed at compile time and
    // both engines at runtime so useJUnitPlatform() discovers and runs everything.
    // Without the vintage engine, the JUnit 4 tests silently don't run ("No tests found").
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    implementation(kotlin("stdlib-jdk8"))
    api("org.jetbrains.teamcity:server-api:2026.1") {
        exclude(group = "org.springframework")
    }
    api("org.jetbrains.teamcity:common-api:2026.1") {
        exclude(group = "org.springframework")
    }
}

// TeamCity 2026.1 requires Java 21 (its API jar is Java 21 bytecode), so build with a JDK 21
// toolchain. This sets the compile/test JVM and target for both Kotlin and Java consistently.
kotlin {
    jvmToolchain(21)
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

changelog {
    version.set(baseVersion)
    path.set(file("CHANGELOG.md").canonicalPath)
    groups.empty()
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    itemPrefix.set("-")
}

val changelogHtml: String by lazy {
    with(changelog) {
        renderItem(
            (getOrNull(baseVersion) ?: getUnreleased())
                .withHeader(false)
                .withEmptySections(false),
            Changelog.OutputType.HTML,
        )
    }
}

tasks.register("generateChangelogHtml") {
    group = "documentation"
    description = "Generates HTML version of the latest changelog entry"

    val outputFile = file("src/main/resources/buildServerResources/changelog.html")
    outputs.file(outputFile)

    doLast {
        val html = """
            |<!DOCTYPE html>
            |<html>
            |<head>
            |    <meta charset="UTF-8">
            |    <title>Team Notify Changelog</title>
            |    <style>
            |        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; line-height: 1.6; }
            |        h1, h2, h3 { color: #333; }
            |        h2 { border-bottom: 1px solid #eee; padding-bottom: 10px; }
            |        h3 { color: #555; }
            |        ul { padding-left: 20px; }
            |        li { margin: 8px 0; }
            |        code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
            |        strong { color: #222; }
            |    </style>
            |</head>
            |<body>
            |    <h1>Team Notify v$baseVersion</h1>
            |    $changelogHtml
            |</body>
            |</html>
        """.trimMargin()
        outputFile.writeText(html)
        println("Generated changelog HTML at: ${outputFile.absolutePath}")
    }
}

tasks.named("processResources") {
    dependsOn("generateChangelogHtml")
}
