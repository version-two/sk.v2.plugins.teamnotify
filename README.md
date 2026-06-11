# TeamNotify - TeamCity Webhook Notifier Plugin

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/versiontwo)

TeamNotify is a TeamCity plugin that sends highly customizable webhook notifications to Slack, Microsoft Teams, and Discord.

## Support Development

If you find TeamNotify useful in your CI/CD workflow, consider supporting its continued development. Your contributions help maintain the plugin, add new features, and ensure compatibility with future TeamCity versions.

[☕ Buy us a coffee on Ko-fi](https://ko-fi.com/versiontwo)

## Features

*   **Multi-Platform:** Slack, Microsoft Teams, Discord.
*   **Per-scope settings:** Configure at the Project or Build Configuration level.
*   **Multiple webhooks:** Define as many as needed per scope.
*   **Versioned Settings Support:** Configure webhooks via Kotlin DSL in your `.teamcity/settings.kts` files.
*   **Rich triggers:**
    *   Build Started
    *   Build Success
    *   Build Failure
    *   Build Stalled
    *   Build Cancelled
    *   Build taking longer than a specified duration
    *   Build taking longer than its average duration
    *   First Build Failure
    *   Build Fixed

## Requirements

*   TeamCity server with API level matching 2026.1 or newer (the plugin targets `2026.1`).
*   Java 21 runtime (TeamCity 2026.1 requires Java 21; the plugin compiles for JVM target 21). Building also requires a JDK 21 toolchain, which Gradle auto-provisions if one is not already installed.

## Building the Plugin

This project uses the TeamCity Gradle plugin. From the project root, run:

```bash
./gradlew clean serverPlugin
# On Windows
gradlew.bat clean serverPlugin
```

The packaged plugin `.zip` will be generated in `build/distributions/`, e.g. `team-notify-1.2.0+<build>-SNAPSHOT.zip`.

## Installing the Plugin

1.  Locate the generated `.zip` file in `build/distributions/` (e.g., `team-notify-1.0-SNAPSHOT.zip`).
2.  Copy this `.zip` file to the `<TeamCity Data Directory>/plugins` directory on your TeamCity server.
3.  Restart the TeamCity server.

## 🔒 Security Best Practices

### **IMPORTANT: Never Hardcode Webhook URLs**

Webhook URLs are sensitive credentials that provide access to your communication channels. Follow these security practices:

*   **Never commit webhook URLs directly in code** - Use TeamCity parameters instead
*   **Use password-type parameters** for webhook URLs to mask them in the UI
*   **Rotate webhook URLs periodically** as part of your security hygiene
*   **Use different webhooks for different environments** (dev, staging, production)
*   **Review version control** for accidentally committed URLs
*   **Set appropriate permissions** on TeamCity parameters containing webhooks

See the [DSL Usage Guide](DSL_USAGE.md) for detailed examples of secure webhook configuration.

## Configuration

### Via TeamCity UI

Once installed, configure webhooks either per Project or per Build Configuration via the TeamCity UI:

1.  Navigate to a Project or Build Configuration.
2.  Open the new tab titled **TeamNotify**.
3.  On this page, you can:
    *   **Add a Webhook:** Provide the webhook URL, select the platform (Slack, Teams, Discord), and pick the conditions under which to notify.
    *   **Test a Webhook:** Use the test action to verify connectivity and payload acceptance.
    *   **View/Delete Webhooks:** Review existing entries and remove any that are no longer needed.

Additionally, an admin page is available under **Administration → TeamNotify** that lists all configured webhooks across projects.

### Via Versioned Settings (Kotlin DSL)

Configure webhooks in your `.teamcity/settings.kts` files:

```kotlin
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildFeature

object MyBuild : BuildType({
    name = "My Build"

    // Define secure parameter for webhook URL
    params {
        password("slack.webhook.url", "",
            label = "Slack Webhook URL",
            display = ParameterDisplay.HIDDEN)
    }

    // Configure webhook using parameter
    features {
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%slack.webhook.url%")  // Never hardcode the URL!
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFailure", "true")
            param("webhook.onSuccess", "true")
        }
    }
})
```

See [DSL_USAGE.md](DSL_USAGE.md) for comprehensive documentation and examples.

### URL validation

For safety, URLs are validated per platform. Expected formats include:

*   Slack: `https://hooks.slack.com/services/...`
*   Microsoft Teams:
    *   Standard: `https://{tenant}.webhook.office.com/webhookb2/...`
    *   Classic: `https://outlook.office.com/webhook/...`
    *   Power Automate: `https://{id}.environment.api.powerplatform.com/...` or `https://{id}.logic.azure.com/...`
*   Discord: `https://discord.com/api/webhooks/{id}/{token}`

> **Note:** Microsoft is retiring Office 365 Connectors (deadline: March 31, 2026). New webhooks should use Power Automate Workflows. The plugin supports optional authentication headers for authenticated Power Automate workflows. See [DSL_USAGE.md](DSL_USAGE.md) for migration details.

## Development

### Project Structure

*   `src/main/kotlin/sk/v2/plugins/teamnotify/`
    *   `controllers/` – UI controllers (`NotifierSettingsController`, `TeamNotifyAdminController`).
    *   `features/` – TeamCity feature descriptors for DSL integration.
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

