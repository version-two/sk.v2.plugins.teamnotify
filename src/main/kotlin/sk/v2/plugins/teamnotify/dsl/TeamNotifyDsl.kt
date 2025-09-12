package sk.v2.plugins.teamnotify.dsl

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.ProjectFeature

/**
 * TeamNotify DSL extension for TeamCity Kotlin DSL
 * Allows configuring webhook notifications in versioned settings
 */

/**
 * Adds TeamNotify webhook configuration to a project
 */
fun Project.teamNotifyWebhook(init: TeamNotifyWebhook.() -> Unit): TeamNotifyWebhook {
    val webhook = TeamNotifyWebhook().apply(init)
    feature(webhook)
    return webhook
}

/**
 * Adds TeamNotify webhook configuration to a build type
 */
fun BuildType.teamNotifyWebhook(init: TeamNotifyWebhook.() -> Unit): TeamNotifyWebhook {
    val webhook = TeamNotifyWebhook().apply(init)
    feature(webhook)
    return webhook
}

/**
 * TeamNotify webhook configuration
 */
class TeamNotifyWebhook : ProjectFeature() {
    init {
        type = "teamnotify.webhook"
    }
    
    /**
     * Webhook URL (required)
     */
    var url by stringParameter("webhook.url")
    
    /**
     * Platform: SLACK, TEAMS, or DISCORD (required)
     */
    var platform by enumParameter<WebhookPlatform>("webhook.platform")
    
    /**
     * Trigger on build start
     */
    var onStart by booleanParameter("webhook.onStart", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger on build success
     */
    var onSuccess by booleanParameter("webhook.onSuccess", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger on build failure
     */
    var onFailure by booleanParameter("webhook.onFailure", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger on build stall
     */
    var onStall by booleanParameter("webhook.onStall", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger on first failure
     */
    var onFirstFailure by booleanParameter("webhook.onFirstFailure", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger when build is fixed
     */
    var onBuildFixed by booleanParameter("webhook.onBuildFixed", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger when build takes longer than average
     */
    var buildLongerThanAverage by booleanParameter("webhook.buildLongerThanAverage", trueValue = "true", falseValue = "false")
    
    /**
     * Trigger when build takes longer than specified seconds
     */
    var buildLongerThan by intParameter("webhook.buildLongerThan")
    
    /**
     * Include recent changes in notifications
     */
    var includeChanges by booleanParameter("webhook.includeChanges", trueValue = "true", falseValue = "false")
    
    /**
     * Enable/disable this webhook
     */
    var enabled by booleanParameter("webhook.enabled", trueValue = "true", falseValue = "false")
    
    /**
     * Configure webhook to send notifications to Slack
     */
    fun slack(url: String, init: SlackWebhookConfig.() -> Unit = {}) {
        this.url = url
        this.platform = WebhookPlatform.SLACK
        SlackWebhookConfig(this).init()
    }
    
    /**
     * Configure webhook to send notifications to Microsoft Teams
     */
    fun teams(url: String, init: TeamsWebhookConfig.() -> Unit = {}) {
        this.url = url
        this.platform = WebhookPlatform.TEAMS
        TeamsWebhookConfig(this).init()
    }
    
    /**
     * Configure webhook to send notifications to Discord
     */
    fun discord(url: String, init: DiscordWebhookConfig.() -> Unit = {}) {
        this.url = url
        this.platform = WebhookPlatform.DISCORD
        DiscordWebhookConfig(this).init()
    }
    
    /**
     * Configure trigger conditions
     */
    fun triggers(init: TriggerConfig.() -> Unit) {
        TriggerConfig(this).init()
    }
}

/**
 * Webhook platform enum
 */
enum class WebhookPlatform {
    SLACK,
    TEAMS,
    DISCORD
}

/**
 * Slack-specific configuration
 */
class SlackWebhookConfig(private val webhook: TeamNotifyWebhook)

/**
 * Teams-specific configuration
 */
class TeamsWebhookConfig(private val webhook: TeamNotifyWebhook)

/**
 * Discord-specific configuration
 */
class DiscordWebhookConfig(private val webhook: TeamNotifyWebhook)

/**
 * Trigger configuration DSL
 */
class TriggerConfig(private val webhook: TeamNotifyWebhook) {
    /**
     * Trigger on build lifecycle events
     */
    fun lifecycle(init: LifecycleTriggers.() -> Unit) {
        LifecycleTriggers(webhook).init()
    }
    
    /**
     * Trigger on build duration conditions
     */
    fun duration(init: DurationTriggers.() -> Unit) {
        DurationTriggers(webhook).init()
    }
    
    /**
     * Trigger on build status changes
     */
    fun statusChanges(init: StatusChangeTriggers.() -> Unit) {
        StatusChangeTriggers(webhook).init()
    }
}

/**
 * Lifecycle event triggers
 */
class LifecycleTriggers(private val webhook: TeamNotifyWebhook) {
    /**
     * Send notification when build starts
     */
    fun onStart() {
        webhook.onStart = true
    }
    
    /**
     * Send notification when build succeeds
     */
    fun onSuccess() {
        webhook.onSuccess = true
    }
    
    /**
     * Send notification when build fails
     */
    fun onFailure() {
        webhook.onFailure = true
    }
    
    /**
     * Send notification when build stalls
     */
    fun onStall() {
        webhook.onStall = true
    }
}

/**
 * Duration-based triggers
 */
class DurationTriggers(private val webhook: TeamNotifyWebhook) {
    /**
     * Send notification when build takes longer than average
     */
    fun longerThanAverage() {
        webhook.buildLongerThanAverage = true
    }
    
    /**
     * Send notification when build takes longer than specified seconds
     */
    fun longerThan(seconds: Int) {
        webhook.buildLongerThan = seconds
    }
}

/**
 * Status change triggers
 */
class StatusChangeTriggers(private val webhook: TeamNotifyWebhook) {
    /**
     * Send notification on first failure
     */
    fun onFirstFailure() {
        webhook.onFirstFailure = true
    }
    
    /**
     * Send notification when build is fixed
     */
    fun onFixed() {
        webhook.onBuildFixed = true
    }
}

// Extension properties for better DSL syntax
private inline fun <reified T : Enum<T>> ProjectFeature.enumParameter(name: String): ParameterDelegate<T?> {
    return object : ParameterDelegate<T?> {
        override operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T? {
            val value = thisRef as ProjectFeature
            return value.param(name)?.let { enumValueOf<T>(it) }
        }
        
        override operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T?) {
            val feature = thisRef as ProjectFeature
            if (value != null) {
                feature.param(name, value.name)
            }
        }
    }
}

private fun ProjectFeature.intParameter(name: String): ParameterDelegate<Int?> {
    return object : ParameterDelegate<Int?> {
        override operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Int? {
            val value = thisRef as ProjectFeature
            return value.param(name)?.toIntOrNull()
        }
        
        override operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int?) {
            val feature = thisRef as ProjectFeature
            if (value != null) {
                feature.param(name, value.toString())
            }
        }
    }
}

interface ParameterDelegate<T> {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T)
}