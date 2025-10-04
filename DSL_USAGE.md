# TeamNotify DSL Usage with Versioned Settings

## Overview
TeamNotify supports configuring webhooks through TeamCity's Kotlin DSL in versioned settings. This allows you to define your webhook notifications as code alongside your build configurations.

## 🔒 Security Best Practices

### **IMPORTANT: Never Hardcode Webhook URLs**
Webhook URLs are sensitive credentials that provide access to your communication channels. **NEVER commit webhook URLs directly in your code**.

### Secure Storage Options

#### 1. Using TeamCity Parameters (Recommended)
Store webhook URLs as TeamCity configuration parameters and reference them in your DSL:

```kotlin
// DO THIS - Use parameters
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%env.SLACK_WEBHOOK_URL%")  // Reference parameter
    param("webhook.platform", "SLACK")
    param("webhook.enabled", "true")
    param("webhook.onFailure", "true")
}

// DON'T DO THIS - Never hardcode URLs
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "https://hooks.slack.com/services/T00/B00/xxxxx")  // INSECURE!
    // ...
}
```

#### 2. Using Password Parameters
For additional security, use password-type parameters that are masked in the UI:

```kotlin
object MyBuild : BuildType({
    params {
        // Define password parameter - value is set in TeamCity UI
        password("slack.webhook.url", "",
            label = "Slack Webhook URL",
            description = "Webhook URL for Slack notifications",
            display = ParameterDisplay.HIDDEN)
    }

    // Use the parameters in webhook configuration
    features {
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%slack.webhook.url%")
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFailure", "true")
        }
    }
})
```

#### 3. Using Root Project Parameters
Define sensitive parameters at the root project level for organization-wide use:

```kotlin
// In root project settings.kts
project {
    params {
        password("org.slack.critical.webhook", "",
            label = "Organization Critical Alerts Webhook",
            display = ParameterDisplay.HIDDEN)
    }
}

// In build type - inherits root parameters
object MyBuild : BuildType({
    features {
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%org.slack.critical.webhook%")  // Uses inherited parameter
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFirstFailure", "true")
        }
    }
})
```

#### 4. Environment-Specific Parameters
Use different parameters for different environments:

```kotlin
object Build : BuildType({
    params {
        // These are set differently per TeamCity instance
        param("webhook.url.dev", "")
        param("webhook.url.staging", "")
        param("webhook.url.prod", "")
        param("env.name", "dev")
    }

    features {
        buildFeature {
            type = "teamnotify.webhook"
            // Use conditional parameter reference based on environment
            param("webhook.url", "%webhook.url.${param("env.name")}%")
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFailure", "true")
        }
    }
})
```

### Security Checklist
- ✅ Store webhook URLs as TeamCity parameters, never in code
- ✅ Use password-type parameters for sensitive URLs
- ✅ Set parameter permissions appropriately in TeamCity
- ✅ Rotate webhook URLs periodically
- ✅ Use different webhooks for different environments
- ✅ Never log or output webhook URLs in build logs
- ✅ Review version control for accidentally committed URLs
- ✅ Use `.gitignore` to exclude local configuration files

## Basic Usage

### Standard Imports
Add the following imports to your `.teamcity/settings.kts` file:

```kotlin
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildFeature
```

### Configure Webhooks at Build Type Level

```kotlin
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildFeature

object MyBuild : BuildType({
    name = "My Build"

    // ... your build configuration ...

    features {
        // Add a Slack webhook
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "https://hooks.slack.com/services/YOUR/WEBHOOK/URL")
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onSuccess", "true")
            param("webhook.onFailure", "true")
            param("webhook.onFirstFailure", "true")
            param("webhook.onBuildFixed", "true")
            param("webhook.buildLongerThanAverage", "true")
            param("webhook.buildLongerThan", "300")  // 5 minutes
        }

        // Add a Microsoft Teams webhook
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "https://outlook.office.com/webhook/YOUR/WEBHOOK/URL")
            param("webhook.platform", "TEAMS")
            param("webhook.enabled", "true")
            param("webhook.onStart", "true")
            param("webhook.onFailure", "true")
        }

        // Add a Discord webhook
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "https://discord.com/api/webhooks/YOUR/WEBHOOK/URL")
            param("webhook.platform", "DISCORD")
            param("webhook.enabled", "true")
            param("webhook.onFirstFailure", "true")
            param("webhook.onBuildFixed", "true")
        }
    }
})
```

### Available Webhook Parameters

All webhook parameters are configured using `param()` inside a `buildFeature` block:

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `webhook.url` | String | Webhook URL (required) | `https://discord.com/api/webhooks/...` |
| `webhook.platform` | String | Platform: `SLACK`, `TEAMS`, `DISCORD` (required) | `DISCORD` |
| `webhook.enabled` | Boolean | Enable/disable webhook | `true` |
| `webhook.onStart` | Boolean | Trigger on build start | `true` |
| `webhook.onSuccess` | Boolean | Trigger on build success | `true` |
| `webhook.onFailure` | Boolean | Trigger on build failure | `true` |
| `webhook.onStall` | Boolean | Trigger on build stall | `true` |
| `webhook.onCancel` | Boolean | Trigger on build cancel | `true` |
| `webhook.onFirstFailure` | Boolean | Trigger on first failure | `true` |
| `webhook.onBuildFixed` | Boolean | Trigger when build is fixed | `true` |
| `webhook.buildLongerThanAverage` | Boolean | Trigger if build takes longer than average | `true` |
| `webhook.buildLongerThan` | Integer | Trigger if build takes longer than N seconds | `600` |
| `webhook.includeChanges` | Boolean | Include change details in notification | `true` |
| `webhook.branchFilter` | String | Filter by branch patterns | `+:main,-:feature/*` |

## Advanced Configuration

### Complete Example with All Options

```kotlin
object MyBuild : BuildType({
    name = "Production Build"

    features {
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%env.DISCORD_WEBHOOK%")
            param("webhook.platform", "DISCORD")
            param("webhook.enabled", "true")

            // Lifecycle triggers
            param("webhook.onStart", "false")
            param("webhook.onSuccess", "true")
            param("webhook.onFailure", "true")
            param("webhook.onStall", "true")
            param("webhook.onCancel", "true")

            // Status change triggers
            param("webhook.onFirstFailure", "true")
            param("webhook.onBuildFixed", "true")

            // Duration triggers
            param("webhook.buildLongerThanAverage", "true")
            param("webhook.buildLongerThan", "300")  // 5 minutes in seconds

            // Additional options
            param("webhook.includeChanges", "true")
            param("webhook.branchFilter", "+:main,+:release/*,-:feature/*")
        }
    }
})
```

### Multiple Webhooks

You can configure multiple webhooks for the same build type:

```kotlin
object MyBuild : BuildType({
    features {
        // Webhook for critical failures to Slack
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%env.SLACK_CRITICAL_WEBHOOK%")
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFailure", "true")
            param("webhook.onFirstFailure", "true")
        }

        // Webhook for all events to Teams
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%env.TEAMS_ALL_EVENTS_WEBHOOK%")
            param("webhook.platform", "TEAMS")
            param("webhook.enabled", "true")
            param("webhook.onStart", "true")
            param("webhook.onSuccess", "true")
            param("webhook.onFailure", "true")
        }
    }
})
```

## Inheritance

Webhooks defined at the project level will be inherited by all build types within that project and its sub-projects. This follows TeamCity's standard inheritance model:

- Webhooks defined at the root project level trigger for all builds
- Webhooks defined at a sub-project level trigger for builds in that sub-project and its children
- Webhooks defined at the build type level trigger only for that specific build

## Combining with UI Configuration

DSL-defined webhooks work alongside webhooks configured through the TeamCity UI:
- Both DSL and UI webhooks will trigger when their conditions are met
- Duplicate webhooks (same URL) are automatically filtered out
- DSL webhooks take precedence when there are duplicates

## Platform-Specific Examples

### Slack
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX")
    param("webhook.platform", "SLACK")
    param("webhook.enabled", "true")
    param("webhook.onSuccess", "true")
    param("webhook.onFailure", "true")
}
```

### Microsoft Teams
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "https://outlook.office.com/webhook/YOUR-GUID/IncomingWebhook/YOUR-WEBHOOK-ID")
    param("webhook.platform", "TEAMS")
    param("webhook.enabled", "true")
    param("webhook.onFailure", "true")
    param("webhook.onFirstFailure", "true")
}
```

### Discord
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "https://discord.com/api/webhooks/1234567890/abcdefghijklmnop")
    param("webhook.platform", "DISCORD")
    param("webhook.enabled", "true")
    param("webhook.onStart", "true")
    param("webhook.onSuccess", "true")
    param("webhook.onFailure", "true")
}
```

## Troubleshooting

1. **Webhooks not triggering**: Ensure the plugin is installed and the build configuration has been committed to version control
2. **Duplicate notifications**: Check for webhooks configured both in DSL and UI with the same URL
3. **Invalid configuration**: The DSL will validate webhook URLs and platforms at configuration time

## Migration from UI Configuration

To migrate existing UI-configured webhooks to DSL:

1. Note down existing webhook configurations from the TeamCity UI
2. Add the equivalent DSL configuration to your `settings.kts`
3. Commit and push the changes
4. Verify webhooks are working
5. Optionally remove the UI-configured webhooks to avoid duplicates