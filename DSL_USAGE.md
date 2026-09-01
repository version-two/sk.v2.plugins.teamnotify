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
    param("webhook.name", "#build-alerts")  // Optional label shown in the UI
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

### How It Works

The TeamNotify plugin registers a build feature with TeamCity using the type `"teamnotify.webhook"`. When TeamCity loads your versioned settings (Kotlin DSL), it processes these build features and stores them alongside webhooks configured through the UI.

**Key Points:**
- Webhooks are defined using `buildFeature` blocks within the `features` section
- Each webhook is a separate `buildFeature` block with `type = "teamnotify.webhook"`
- You can define multiple webhooks per build type
- DSL-defined webhooks are treated the same as UI-defined webhooks at runtime
- Webhooks are stored in TeamCity's project settings (not in the plugin's database)

### Required Imports
Add the following imports to your `.teamcity/settings.kts` file:

```kotlin
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildFeature
```

**Note:** TeamNotify does not require custom DSL imports. The plugin uses TeamCity's standard build feature system.

### Configure Webhooks at Build Type Level

```kotlin
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildFeature

object MyBuild : BuildType({
    name = "My Build"

    // ... your build configuration ...

    features {
        // Add a Slack webhook using a parameter reference (recommended)
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%slack.webhook.url%")  // Reference to TeamCity parameter
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onSuccess", "true")
            param("webhook.onFailure", "true")
            param("webhook.onFirstFailure", "true")
            param("webhook.onBuildFixed", "true")
            param("webhook.buildLongerThanAverage", "true")
            param("webhook.buildLongerThan", "300")  // 5 minutes in seconds
        }

        // Add a Microsoft Teams webhook
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%teams.webhook.url%")
            param("webhook.platform", "TEAMS")
            param("webhook.enabled", "true")
            param("webhook.onStart", "true")
            param("webhook.onFailure", "true")
        }

        // Add a Discord webhook with branch filtering
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%discord.webhook.url%")
            param("webhook.platform", "DISCORD")
            param("webhook.enabled", "true")
            param("webhook.onFirstFailure", "true")
            param("webhook.onBuildFixed", "true")
            param("webhook.branchFilter", "+:main,+:release/*")  // Only main and release branches
        }
    }
})
```

### Available Webhook Parameters

All webhook parameters are configured using `param()` inside a `buildFeature` block:

| Parameter | Type | Required | Default | Description | Example |
|-----------|------|----------|---------|-------------|---------|
| `webhook.url` | String | **Yes** | - | Webhook URL | `https://discord.com/api/webhooks/...` |
| `webhook.platform` | String | **Yes** | - | Platform: `SLACK`, `TEAMS`, or `DISCORD` | `DISCORD` |
| `webhook.name` | String | No | `null` | Display name shown in the TeamNotify UI lists (URLs stay masked) | `"#build-alerts"` |
| `webhook.enabled` | String | No | `"true"` | Enable/disable webhook | `"true"` or `"false"` |
| `webhook.onStart` | String | No | `"false"` | Trigger on build start | `"true"` or `"false"` |
| `webhook.onSuccess` | String | No | `"false"` | Trigger on build success | `"true"` or `"false"` |
| `webhook.onFailure` | String | No | `"true"` | Trigger on build failure | `"true"` or `"false"` |
| `webhook.onStall` | String | No | `"false"` | Trigger on build stall | `"true"` or `"false"` |
| `webhook.onCancel` | String | No | `"false"` | Trigger on build cancel | `"true"` or `"false"` |
| `webhook.onFirstFailure` | String | No | `"false"` | Trigger on first failure after success | `"true"` or `"false"` |
| `webhook.onBuildFixed` | String | No | `"false"` | Trigger when build is fixed after failure | `"true"` or `"false"` |
| `webhook.buildLongerThanAverage` | String | No | `"false"` | Trigger if build duration exceeds average | `"true"` or `"false"` |
| `webhook.buildLongerThan` | String | No | `null` | Trigger if build takes longer than N seconds | `"300"` (for 5 minutes) |
| `webhook.includeChanges` | String | No | `"true"` | Include commit details in notification | `"true"` or `"false"` |
| `webhook.branchFilter` | String | No | `null` | Filter by branch patterns (comma-separated) | `"+:main,+:release/*,-:feature/*"` |
| `webhook.authHeaderName` | String | No | `null` | Authentication header name (for Power Automate) | `"Authorization"` |
| `webhook.authHeaderValue` | String | No | `null` | Authentication header value (for Power Automate) | `"%teams.auth.token%"` |

**Important Notes:**
- All parameter values must be strings (in quotes), even for boolean and numeric values
- The `buildFeature` uses `param()` which accepts string key-value pairs
- Boolean values are converted internally: `"true"` → true, `"false"` → false
- At least one trigger condition should be enabled for the webhook to be useful
- Branch filter uses include (`+:`) and exclude (`-:`) patterns with wildcards (`*`)

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

## How Webhooks Are Stored

### Storage Mechanism
The plugin uses TeamCity's `ProjectSettingsManager` to store webhook configurations. All UI-defined
data for a project lives in a single settings object under the key `"team-notify.settings"`:
- **Project webhooks**: the project-level webhook list
- **Build type webhooks**: keyed by build type id inside the same settings object (there is no
  separate `team-notify.settings.{buildTypeId}` key)
- **Locally-disabled inherited webhooks**: tracked per build type inside the same settings object
- **DSL webhooks**: parsed from `teamnotify.webhook` build features when the configuration is loaded

### Storage Format
Webhooks are stored in a `TeamNotifyProjectSettings` object containing `WebhookConfiguration` instances. Each configuration includes:
- `url`: The webhook URL (String)
- `platform`: The platform enum (SLACK, TEAMS, or DISCORD)
- `enabled`: Whether the webhook is active (Boolean)
- All trigger conditions (Boolean flags)
- Optional settings like `buildLongerThan`, `branchFilter`, etc.

### Retrieval Priority
When a build runs, the plugin retrieves webhooks in this order:
1. **DSL-defined webhooks** from the build type's features
2. **Build type-specific webhooks** from UI configuration
3. **Project webhooks** from the project hierarchy (traversing up to root)
4. Duplicates (same URL) are filtered out, with DSL webhooks taking precedence

## Inheritance

### Project-Level Webhooks
You can define webhooks at the project level to apply them to all build types within that project and its sub-projects. However, **DSL does not currently support defining build features at the project level**. For project-level webhooks, use the TeamCity UI.

### Build Type-Level Webhooks
Webhooks defined at the build type level (in DSL or UI) apply only to that specific build configuration:
- Build type webhooks defined in DSL will be stored and executed for that build type
- These webhooks are inherited by no other build types
- They can be disabled locally (for inherited webhooks) or deleted entirely

### Inheritance Behavior
- Project webhooks configured via UI are inherited by all child projects and build types
- DSL webhooks defined in a build type apply only to that build type
- When a webhook with the same URL exists at multiple levels, the most specific one takes precedence
- You can locally disable inherited webhooks in build configurations without deleting them

## Combining with UI Configuration

DSL-defined webhooks work seamlessly alongside webhooks configured through the TeamCity UI:

**How it works:**
- Both DSL and UI webhooks are loaded when a build runs
- All webhooks are merged into a single list
- Duplicate webhooks (same URL) are automatically filtered using `distinctBy { it.url }`
- The merged list contains unique webhooks from all sources

**Example scenario:**
```
Project "MyProject" (UI):
  - Webhook A (Slack, failures only)

Build Type "MyBuild" (DSL):
  - Webhook A (Slack, all events)  ← Same URL, DSL takes precedence
  - Webhook B (Discord, failures only)

Result:
  - Webhook A with DSL configuration (all events)
  - Webhook B from DSL
```

**Best Practices:**
- Use DSL for build-specific notifications
- Use UI for project-wide or organizational notifications
- Avoid duplicate URLs unless you want DSL to override UI configuration

## Platform-Specific Examples

### Slack
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%slack.webhook.url%")
    param("webhook.platform", "SLACK")
    param("webhook.enabled", "true")
    param("webhook.onSuccess", "true")
    param("webhook.onFailure", "true")
    param("webhook.includeChanges", "true")
}
```

**Slack webhook URL format:** `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX`

### Microsoft Teams
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%teams.webhook.url%")
    param("webhook.platform", "TEAMS")
    param("webhook.enabled", "true")
    param("webhook.onFailure", "true")
    param("webhook.onFirstFailure", "true")
    param("webhook.onBuildFixed", "true")
}
```

**Teams webhook URL formats (as of 2026):**
- Power Automate (New, recommended): `https://{id}.environment.api.powerplatform.com/powerautomate/automations/direct/workflows/...`
- Workflows via `webhook.office.com`: `https://{tenant}.webhook.office.com/webhookb2/{guid}/IncomingWebhook/{id}/{guid}`
- Power Automate (Legacy `logic.azure.com`): `https://{id}.logic.azure.com/workflows/...` – **stopped delivering Nov 30, 2025**; Microsoft migrated these flows to the `powerplatform.com` host. Re-create the workflow to obtain a current URL.
- Classic O365 Connector: `https://outlook.office.com/webhook/{guid}/IncomingWebhook/{id}/{guid}` – legacy connector, retiring (see below).

**Note:** Microsoft is transitioning to Power Automate-based workflows. The new format using `environment.api.powerplatform.com` is the recommended approach for new webhooks.

### Microsoft Teams Power Automate Migration

Microsoft is retiring Office 365 Connectors within Microsoft Teams. The final cutoff is **May 18–22, 2026** (rollout begins May 18, completes May 22); after this, connector webhooks stop working entirely. (Separately, Power Automate flows on the legacy `logic.azure.com` host already stopped working on November 30, 2025 – use the `powerplatform.com` host instead.)

**Migration Steps:**
1. Create a new Power Automate Workflow in Microsoft Teams
2. Add the "When a Teams webhook request is received" trigger
3. Configure authentication (see below)
4. Add a "Post message in a chat or channel" action
5. Update your TeamNotify webhook URL to the new Power Automate URL

**Authentication for Power Automate Workflows:**

When creating a Power Automate Workflow, you can choose the authentication method:
- **"Anyone"**: No authentication required (simplest option)
- **"Any user in my tenant"**: Requires a Bearer token

For authenticated workflows, configure the auth header in your DSL:

```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%teams.powerautomate.url%")
    param("webhook.platform", "TEAMS")
    param("webhook.enabled", "true")
    param("webhook.onFailure", "true")
    // Authentication for Power Automate (optional - only for authenticated workflows)
    param("webhook.authHeaderName", "Authorization")
    param("webhook.authHeaderValue", "%teams.powerautomate.token%")  // Store token as password parameter
}
```

**Important:** Store the authentication token as a password-type parameter in TeamCity to keep it secure.

### Discord
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%discord.webhook.url%")
    param("webhook.platform", "DISCORD")
    param("webhook.enabled", "true")
    param("webhook.onStart", "true")
    param("webhook.onSuccess", "true")
    param("webhook.onFailure", "true")
    param("webhook.includeChanges", "true")
}
```

**Discord webhook URL format:** `https://discord.com/api/webhooks/{id}/{token}` or `https://discordapp.com/api/webhooks/{id}/{token}`

## Troubleshooting

### Common Issues

#### Webhooks not triggering
**Symptoms:** DSL-defined webhooks don't send notifications
**Causes & Solutions:**
- **Plugin not installed**: Verify the TeamNotify plugin is installed and enabled in TeamCity
- **Settings not committed**: Ensure your DSL changes are committed to version control and loaded by TeamCity
- **Invalid configuration**: Check TeamCity logs for validation errors (invalid URL, unknown platform, etc.)
- **No triggers enabled**: At least one trigger condition must be set to `"true"`
- **Branch filter mismatch**: If `webhook.branchFilter` is set, verify the build's branch matches the pattern

#### Duplicate notifications
**Symptoms:** Receiving multiple notifications for the same build event
**Causes & Solutions:**
- **Same URL in DSL and UI**: Remove the duplicate from either DSL or UI configuration
- **Multiple build features**: Check if you accidentally defined the same webhook twice in DSL
- To debug: View the webhook list in the TeamCity UI to see all active webhooks

#### Invalid configuration errors
**Symptoms:** Build configuration fails to load or shows validation errors
**Causes & Solutions:**
- **Invalid platform**: Must be exactly `"SLACK"`, `"TEAMS"`, or `"DISCORD"` (case-sensitive)
- **Missing required parameters**: `webhook.url` and `webhook.platform` are required
- **Invalid URL format**: Each platform has specific URL format requirements (see Platform-Specific Examples)
- **Invalid branch filter**: Branch filters must follow TeamCity's pattern syntax (e.g., `"+:main,-:feature/*"`)

#### Parameter references not resolving
**Symptoms:** Webhook URL shows as literal `%parameter.name%` instead of the actual value
**Causes & Solutions:**
- **Parameter not defined**: Ensure the parameter exists in the build type, project, or root project
- **Wrong parameter name**: Check for typos in the parameter reference
- **Parameter not inherited**: Verify the parameter is accessible from the build configuration's scope

### Debugging Tips

1. **Check TeamCity logs**: Look for TeamNotify plugin logs in `teamcity-server.log`
2. **Verify webhook storage**: Go to TeamCity UI → Project Settings → Team Notify to see stored webhooks
3. **Test webhook URL**: Use the "Test Connection" feature in the TeamCity UI before adding to DSL
4. **Use verbose logging**: Enable debug logging for `sk.v2.plugins.teamnotify` package
5. **Check build features**: In TeamCity UI, view the build configuration's features to confirm DSL webhooks are loaded

### Validation

The plugin validates webhook configurations when they are loaded:
- **URL validation**: Checks if the URL matches the expected format for the platform
- **Platform validation**: Ensures the platform is one of the supported values
- **Parameter validation**: Converts string parameters to appropriate types (boolean, integer)

If validation fails, the webhook may be skipped or cause a configuration error. Check TeamCity's server logs for details.

## Migration from UI Configuration

To migrate existing UI-configured webhooks to DSL:

1. Note down existing webhook configurations from the TeamCity UI
2. Add the equivalent DSL configuration to your `settings.kts`
3. Commit and push the changes
4. Verify webhooks are working
5. Optionally remove the UI-configured webhooks to avoid duplicates
