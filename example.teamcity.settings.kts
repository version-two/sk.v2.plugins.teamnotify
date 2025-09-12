/*
 * Example TeamCity Kotlin DSL configuration showing how to use TeamNotify webhooks
 * including the new onCancel trigger introduced in v1.2.0
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.buildFeature

version = "2023.11"

project {
    
    // Define webhook as a build feature in your build type
    buildType {
        id("MyBuildType")
        name = "My Build Configuration"
        
        features {
            // Add TeamNotify webhook feature
            buildFeature {
                type = "teamnotify.webhook"
                param("webhook.url", "%env.SLACK_WEBHOOK_URL%")  // Use parameter for security
                param("webhook.platform", "SLACK")
                param("webhook.enabled", "true")
                
                // Trigger configuration
                param("webhook.onStart", "true")
                param("webhook.onSuccess", "true")
                param("webhook.onFailure", "true")
                param("webhook.onCancel", "true")        // New in v1.2.0
                param("webhook.onStall", "false")
                param("webhook.onFirstFailure", "true")
                param("webhook.onBuildFixed", "true")
                
                // Duration triggers
                param("webhook.buildLongerThanAverage", "false")
                param("webhook.buildLongerThan", "")     // Empty means disabled
                
                // Additional options
                param("webhook.includeChanges", "true")
                param("webhook.branchFilter", "main,develop,release/*")
            }
            
            // You can add multiple webhooks for different platforms
            buildFeature {
                type = "teamnotify.webhook"
                param("webhook.url", "%env.TEAMS_WEBHOOK_URL%")
                param("webhook.platform", "TEAMS")
                param("webhook.enabled", "true")
                param("webhook.onFailure", "true")
                param("webhook.onCancel", "true")        // New in v1.2.0
                param("webhook.onFirstFailure", "true")
                param("webhook.includeChanges", "false")
            }
            
            // Discord webhook with all triggers
            buildFeature {
                type = "teamnotify.webhook"
                param("webhook.url", "%env.DISCORD_WEBHOOK_URL%")
                param("webhook.platform", "DISCORD")
                param("webhook.enabled", "true")
                param("webhook.onStart", "true")
                param("webhook.onSuccess", "true")
                param("webhook.onFailure", "true")
                param("webhook.onCancel", "true")        // New in v1.2.0
                param("webhook.onStall", "true")
                param("webhook.onFirstFailure", "true")
                param("webhook.onBuildFixed", "true")
                param("webhook.buildLongerThanAverage", "true")
                param("webhook.buildLongerThan", "300")  // 5 minutes
                param("webhook.includeChanges", "true")
                param("webhook.branchFilter", "+:*,-:feature/*")  // All except feature branches
            }
        }
        
        // Your build steps...
        steps {
            // ...
        }
    }
    
    // Project-level webhook (inherited by all build types)
    features {
        buildFeature {
            type = "teamnotify.webhook"
            param("webhook.url", "%env.PROJECT_SLACK_WEBHOOK%")
            param("webhook.platform", "SLACK")
            param("webhook.enabled", "true")
            param("webhook.onFailure", "true")
            param("webhook.onCancel", "true")            // New in v1.2.0
            param("webhook.onFirstFailure", "true")
            param("webhook.includeChanges", "true")
        }
    }
}