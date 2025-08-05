# TeamNotify - TeamCity Webhook Notifier Plugin

TeamNotify is a TeamCity CI/CD plugin designed to provide highly customizable webhook-based notifications to various platforms like Slack, Microsoft Teams, and Discord.

## Features

*   **Multi-Platform Support:** Send notifications to Slack, Microsoft Teams, and Discord.
*   **Granular Configuration:** Configure webhooks at the project or build configuration level.
*   **Multiple Webhook Definitions:** Define multiple webhooks for different conditions and platforms.
*   **Rich Conditional Triggers:** Trigger notifications based on:
    *   Build Success
    *   Build Failure
    *   Build Stalled
    *   Build taking longer than a specified duration
    *   Build taking longer than its average duration
    *   First Build Failure
    *   Build Fixed

## Building the Plugin

This plugin is built using Gradle. To build the plugin, navigate to the root directory of the project and run the following command:

```bash
./gradlew clean buildPlugin
```

This will generate a `.zip` file in the `build/distributions/` directory, which is your plugin package.

## Installing the Plugin

1.  Locate the generated `.zip` file in `build/distributions/` (e.g., `team-notify-1.0-SNAPSHOT.zip`).
2.  Copy this `.zip` file to the `<TeamCity Data Directory>/plugins` directory on your TeamCity server.
3.  Restart the TeamCity server.

## Configuration

Once installed, you can configure webhooks within your TeamCity projects or build configurations:

1.  Navigate to a **Project** or **Build Configuration** in TeamCity.
2.  Go to **Edit Project Settings** or **Edit Build Configuration**.
3.  Look for a new tab or section named **"Webhook Notifier Settings"**.
4.  On this page, you can:
    *   **Add New Webhook:** Provide the webhook URL, select the target platform (Slack, Teams, Discord), and choose the conditions under which the notification should be sent.
    *   **View Existing Webhooks:** See a list of all configured webhooks for the current project/build configuration.
    *   **Delete Webhooks:** Remove unwanted webhook configurations.

## Development

### Project Structure

*   `src/main/kotlin/sk/v2/plugins/teamnotify/`: Main Kotlin source code.
    *   `controllers/`: Spring MVC controllers for UI pages.
    *   `listeners/`: TeamCity `BuildServerAdapter` implementations for build event handling.
    *   `model/`: Data classes for webhook configurations.
    *   `payloads/`: Logic for generating platform-specific webhook payloads.
    *   `services/`: Core business logic, including `WebhookManager`, `WebhookService`, `BuildStallTracker`, and `BuildDurationService`.
*   `src/main/resources/META-INF/build-server-plugin.xml`: Plugin descriptor.
*   `src/main/resources/editNotifierSettings.jsp`: JSP for the plugin's settings UI.
*   `src/test/kotlin/`: Unit tests.

### Running a Local TeamCity Server for Development

You can run a local TeamCity server with your plugin installed using Gradle:

```bash
./gradlew server
```

The server will typically be available at `http://localhost:8111`.

