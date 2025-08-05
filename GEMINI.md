# Gemini Project Context: TeamCity Notifier Plugin

This file provides context for Gemini to understand the project and assist with the development of the TeamCity Webhook Notifier plugin.

## Project Overview

This project is a **TeamCity CI/CD plugin** designed to provide highly customizable webhook-based notifications. It allows administrators and project managers to configure notification settings for each project or build configuration, sending alerts to platforms like **Slack, Microsoft Teams, and Discord**.

The core functionality is to define multiple webhook endpoints and associate them with specific build events and conditions. This enables granular control over what notifications are sent and where. For example, a team could send detailed failure logs to a private Discord channel while sending simple success messages to a public Slack channel.

The plugin is likely being developed in **Kotlin** using the standard TeamCity Open API and will be packaged as a `.zip` file for installation.

## Key Features

*   **Multi-Platform Support:** Natively supports sending webhooks to Slack, Microsoft Teams, and Discord. The architecture should be extensible for future platforms.
*   **Granular Configuration:** Settings can be applied at both the Project and Build Configuration levels in TeamCity.
*   **Multiple Webhook Definitions:** Users can define multiple webhook URLs for each channel (e.g., one for successes, one for failures).
*   **Rich Conditional Triggers:** Notifications can be triggered based on a wide range of build events and conditions:

    ### Standard Triggers
    *   **On Build Start:** When a build is taken from the queue and starts running.
    *   **On Build Success:** When a build finishes with a "success" status.
    *   **On Build Failure:** When a build finishes with a "failure" status.
    *   **On Build Finish:** When a build finishes, regardless of its final status.

    ### State-Change Triggers
    *   **On Build Fixed:** The first successful build after one or more failed builds.
    *   **On First Build Failure:** The first failed build after a series of successful builds.
    *   **On New Test Failures:** When a build fails with tests that were not failing in the previous build.

    ### Resource-Based Triggers
    *   **On Build Stalled:** When a build stops logging output for a configurable amount of time.
    *   **If Build Takes More Than $time:** When a build's duration exceeds a user-defined static threshold (e.g., > 20 minutes).
    *   **If Build Takes Longer Than Average:** When a build's duration exceeds the calculated average run time for that build configuration.

## Building and Running

*(Note: These are inferred commands based on a typical TeamCity plugin setup using Gradle. Please update if the project uses Maven or another build system.)*

```bash
# Clean the project and remove old artifacts
./gradlew clean

# Build the plugin .zip package
# The output will be in build/distributions/
./gradlew buildPlugin

# Run a local TeamCity server instance with the plugin installed for testing
# The server will be available at http://localhost:8111
./gradlew server
```

## Development Conventions

*   **Language:** Kotlin. Adhere to the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
*   **Testing:** Use JUnit 5 for unit and integration tests. Mocking can be done with MockK.
*   **Dependency Management:** Dependencies are managed via `build.gradle.kts`.
*   **UI:** The user-facing configuration pages are built with JSP (`.jsp`) files, interacting with Spring MVC controllers on the backend.

## Key Files & Directory Structure

*   `build.gradle.kts`: The main Gradle build script for managing dependencies and build tasks.
*   `src/main/kotlin/com/example/dcnotify/`: The root package for the plugin's source code.
*   `src/main/kotlin/com/example/dcnotify/controllers/`: Spring controllers that handle the plugin's UI settings pages (e.g., saving webhook configurations).
*   `src/main/kotlin/com/example/dcnotify/listeners/`: Core TeamCity event listeners (`BuildServerListener`) that react to build events and trigger the notification logic.
*   `src/main/kotlin/com/example/dcnotify/services/`: Services for handling webhook sending, payload formatting, and condition evaluation.
*   `src/main/resources/META-INF/build-server-plugin.xml`: The primary plugin descriptor file that registers the plugin with TeamCity.
*   `src/main/resources/editProjectSettings.jsp`: The JSP view for the webhook settings page at the project level.
*   `src/main/resources/editBuildSettings.jsp`: The JSP view for the webhook settings page at the build configuration level.
*   `src/test/kotlin/`: Source for all unit and integration tests.
