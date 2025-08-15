# TeamNotify - TeamCity Webhook Notifier Plugin

TeamNotify is a TeamCity plugin that sends highly customizable webhook notifications to Slack, Microsoft Teams, and Discord.

## Features

*   **Multi-Platform:** Slack, Microsoft Teams, Discord.
*   **Per-scope settings:** Configure at the Project or Build Configuration level.
*   **Multiple webhooks:** Define as many as needed per scope.
*   **Rich triggers:**
    *   Build Started
    *   Build Success
    *   Build Failure
    *   Build Stalled
    *   Build taking longer than a specified duration
    *   Build taking longer than its average duration
    *   First Build Failure
    *   Build Fixed

## Requirements

*   TeamCity server with API level matching 2025.07 or newer (the plugin targets `2025.07`).
*   Java 17 runtime (the plugin compiles for JVM target 17).

## Building the Plugin

This project uses the TeamCity Gradle plugin. From the project root, run:

```bash
./gradlew clean serverPlugin
# On Windows
gradlew.bat clean serverPlugin
```

The packaged plugin `.zip` will be generated in `build/distributions/`, e.g. `team-notify-1.1.0+<build>-SNAPSHOT.zip`.

## Installing the Plugin

1.  Locate the generated `.zip` file in `build/distributions/` (e.g., `team-notify-1.0-SNAPSHOT.zip`).
2.  Copy this `.zip` file to the `<TeamCity Data Directory>/plugins` directory on your TeamCity server.
3.  Restart the TeamCity server.

## Configuration

Once installed, configure webhooks either per Project or per Build Configuration via the TeamCity UI:

1.  Navigate to a Project or Build Configuration.
2.  Open the new tab titled **TeamNotify**.
3.  On this page, you can:
    *   **Add a Webhook:** Provide the webhook URL, select the platform (Slack, Teams, Discord), and pick the conditions under which to notify.
    *   **Test a Webhook:** Use the test action to verify connectivity and payload acceptance.
    *   **View/Delete Webhooks:** Review existing entries and remove any that are no longer needed.

Additionally, an admin page is available under **Administration → TeamNotify** that lists all configured webhooks across projects.

### URL validation

For safety, URLs are validated per platform. Expected formats include:

*   Slack: `https://hooks.slack.com/services/...`
*   Microsoft Teams: `https://{tenant}.webhook.office.com/webhookb2/...` or `https://outlook.office.com/...`
*   Discord: `https://discord.com/api/webhooks/{id}/{token}`

## Development

### Project Structure

*   `src/main/kotlin/sk/v2/plugins/teamnotify/`
    *   `controllers/` – UI controllers (`NotifierSettingsController`, `TeamNotifyAdminController`).
    *   `listeners/` – Build event handling.
    *   `model/` – Configuration models.
    *   `payloads/` – Platform-specific payload generators (Slack/Teams/Discord).
    *   `services/` – Core services: `WebhookManager`, `WebhookService`, `BuildStallTracker`, `BuildDurationService`, timers.
    *   `extensions/` – UI tabs and admin page extensions.
    *   `settings/` – Settings factory/registrar.
*   `src/main/resources/META-INF/teamcity-plugin.xml` – Plugin descriptor (ZIP root).
*   `src/main/resources/META-INF/spring-plugin.xml` – Spring beans.
*   `src/main/resources/buildServerResources/` – Web UI resources (JSP, CSS).
*   `src/test/kotlin/` – Tests.

### Notes

* Settings are stored per project using TeamCity Project Settings.
* The plugin does not declare TeamCity core services as Spring beans; they are injected by TeamCity.

