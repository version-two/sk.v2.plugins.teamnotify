# TeamNotify DSL Usage with Versioned Settings

## Overview
TeamNotify now supports configuring webhooks through TeamCity's Kotlin DSL in versioned settings. This allows you to define your webhook notifications as code alongside your build configurations.

## 🔒 Security Best Practices

### **IMPORTANT: Never Hardcode Webhook URLs**
Webhook URLs are sensitive credentials that provide access to your communication channels. **NEVER commit webhook URLs directly in your code**.

### Secure Storage Options

#### 1. Using TeamCity Parameters (Recommended)
Store webhook URLs as TeamCity configuration parameters and reference them in your DSL:

```kotlin
// DO THIS - Use parameters
teamNotifyWebhook {
    slack(param("slack.webhook.url"))  // Reference parameter
    triggers {
        lifecycle {
            onFailure()
        }
    }
}

// DON'T DO THIS - Never hardcode URLs
teamNotifyWebhook {
    slack("https://hooks.slack.com/services/T00/B00/xxxxx")  // INSECURE!
    // ...
}
```

#### 2. Using Password Parameters
For additional security, use password-type parameters that are masked in the UI:

```kotlin
object Project : Project({
    params {
        // Define password parameter - value is set in TeamCity UI
        password("slack.webhook.url", "", 
            label = "Slack Webhook URL",
            description = "Webhook URL for Slack notifications",
            display = ParameterDisplay.HIDDEN)
        
        password("teams.webhook.url", "",
            label = "Teams Webhook URL", 
            description = "Webhook URL for Teams notifications",
            display = ParameterDisplay.HIDDEN)
    }
    
    // Use the parameters in webhook configuration
    teamNotifyWebhook {
        slack(param("slack.webhook.url"))
        triggers {
            lifecycle {
                onFailure()
            }
        }
    }
})
```

#### 3. Using Root Project Parameters
Define sensitive parameters at the root project level for organization-wide use:

```kotlin
// In root project settings.kts
object RootProject : Project({
    params {
        password("org.slack.critical.webhook", "", 
            label = "Organization Critical Alerts Webhook",
            display = ParameterDisplay.HIDDEN)
    }
})

// In sub-project - inherits root parameters
object SubProject : Project({
    teamNotifyWebhook {
        slack(param("org.slack.critical.webhook"))  // Uses inherited parameter
        triggers {
            statusChanges {
                onFirstFailure()
            }
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
    }
    
    teamNotifyWebhook {
        // Select webhook based on environment
        val webhookUrl = when (param("env.name")) {
            "production" -> param("webhook.url.prod")
            "staging" -> param("webhook.url.staging")
            else -> param("webhook.url.dev")
        }
        
        slack(webhookUrl)
        triggers {
            lifecycle {
                onFailure()
            }
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

### Import the DSL
Add the following import to your `.teamcity/settings.kts` file:

```kotlin
import sk.v2.plugins.teamnotify.dsl.*
```

### Configure Webhooks at Project Level

```kotlin
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import sk.v2.plugins.teamnotify.dsl.*

object Project : Project({
    // ... your project configuration ...
    
    // Add a Slack webhook
    teamNotifyWebhook {
        slack("https://hooks.slack.com/services/YOUR/WEBHOOK/URL") {
            // Configuration specific to Slack
        }
        
        triggers {
            lifecycle {
                onSuccess()
                onFailure()
                onFirstFailure()
                onFixed()
            }
            
            duration {
                longerThanAverage()
                longerThan(300) // 5 minutes
            }
        }
        
        enabled = true
    }
    
    // Add a Microsoft Teams webhook
    teamNotifyWebhook {
        teams("https://outlook.office.com/webhook/YOUR/WEBHOOK/URL") {
            // Configuration specific to Teams
        }
        
        triggers {
            lifecycle {
                onStart()
                onFailure()
            }
        }
    }
    
    // Add a Discord webhook
    teamNotifyWebhook {
        discord("https://discord.com/api/webhooks/YOUR/WEBHOOK/URL") {
            // Configuration specific to Discord
        }
        
        triggers {
            statusChanges {
                onFirstFailure()
                onFixed()
            }
        }
    }
})
```

### Configure Webhooks at Build Type Level

```kotlin
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import sk.v2.plugins.teamnotify.dsl.*

object Build : BuildType({
    name = "My Build"
    
    // ... your build configuration ...
    
    // Add webhook specific to this build type
    teamNotifyWebhook {
        url = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
        platform = WebhookPlatform.SLACK
        
        // Individual trigger flags
        onStart = false
        onSuccess = true
        onFailure = true
        onStall = false
        onFirstFailure = true
        onBuildFixed = true
        buildLongerThanAverage = true
        buildLongerThan = 600 // 10 minutes
        
        enabled = true
    }
})
```

## Advanced Configuration

### Using Fluent API for Triggers

The DSL provides a fluent API for configuring triggers:

```kotlin
teamNotifyWebhook {
    slack("https://hooks.slack.com/services/YOUR/WEBHOOK/URL")
    
    triggers {
        // Lifecycle events
        lifecycle {
            onStart()       // When build starts
            onSuccess()     // When build succeeds
            onFailure()     // When build fails
            onStall()       // When build stalls
        }
        
        // Duration-based triggers
        duration {
            longerThanAverage()    // When build takes longer than average
            longerThan(300)        // When build takes longer than 5 minutes
        }
        
        // Status change triggers
        statusChanges {
            onFirstFailure()       // First failure after success
            onFixed()              // Build fixed after failure
        }
    }
}
```

### Multiple Webhooks

You can configure multiple webhooks for the same project or build type:

```kotlin
object Project : Project({
    // Webhook for critical failures
    teamNotifyWebhook {
        slack("https://hooks.slack.com/services/CRITICAL/WEBHOOK/URL")
        triggers {
            lifecycle {
                onFailure()
            }
            statusChanges {
                onFirstFailure()
            }
        }
    }
    
    // Webhook for all events
    teamNotifyWebhook {
        teams("https://outlook.office.com/webhook/ALL/EVENTS/URL")
        triggers {
            lifecycle {
                onStart()
                onSuccess()
                onFailure()
            }
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
teamNotifyWebhook {
    slack("https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX")
    triggers {
        lifecycle {
            onSuccess()
            onFailure()
        }
    }
}
```

### Microsoft Teams
```kotlin
teamNotifyWebhook {
    teams("https://outlook.office.com/webhook/YOUR-GUID/IncomingWebhook/YOUR-WEBHOOK-ID")
    triggers {
        lifecycle {
            onFailure()
        }
        statusChanges {
            onFirstFailure()
        }
    }
}
```

### Discord
```kotlin
teamNotifyWebhook {
    discord("https://discord.com/api/webhooks/1234567890/abcdefghijklmnop")
    triggers {
        lifecycle {
            onStart()
            onSuccess()
            onFailure()
        }
    }
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