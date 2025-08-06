import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.github.rodm.teamcity-server") version "1.5.2"
}

group = "sk.v2.plugins.teamnotify"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/teamcity/teamcity-api")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    implementation(kotlin("stdlib-jdk8"))
    api("org.jetbrains.teamcity:server-api:2025.07")
    api("org.jetbrains.teamcity:common-api:2025.07")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Spring dependencies required by our plugin
    implementation("org.springframework:spring-beans:5.3.39")
    implementation("org.springframework:spring-webmvc:5.3.39")
    implementation("org.springframework:spring-context:5.3.39")
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



