package sk.v2.plugins.teamnotify.features

import jetbrains.buildServer.serverSide.BuildFeature
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.web.openapi.PluginDescriptor

/**
 * Feature descriptor for TeamNotify webhooks defined through DSL
 * This allows webhooks to be configured in versioned settings
 */
class TeamNotifyWebhookFeature(
    private val pluginDescriptor: PluginDescriptor
) : BuildFeature() {
    
    override fun getType(): String = "teamnotify.webhook"
    
    override fun getDisplayName(): String = "TeamNotify Webhook"
    
    override fun getEditParametersUrl(): String? = 
        pluginDescriptor.getPluginResourcesPath("editWebhookFeature.jsp")
    
    override fun isMultipleFeaturesPerBuildTypeAllowed(): Boolean = true
    
    override fun getParametersProcessor(): PropertiesProcessor? {
        return PropertiesProcessor { properties ->
            val errors = mutableListOf<InvalidProperty>()
            
            // Validate webhook URL
            val url = properties["webhook.url"]
            if (url.isNullOrBlank()) {
                errors.add(InvalidProperty("webhook.url", "Webhook URL is required"))
            }
            
            // Validate platform
            val platform = properties["webhook.platform"]
            if (platform.isNullOrBlank()) {
                errors.add(InvalidProperty("webhook.platform", "Platform is required"))
            } else {
                try {
                    sk.v2.plugins.teamnotify.model.WebhookPlatform.valueOf(platform)
                } catch (e: Exception) {
                    errors.add(InvalidProperty("webhook.platform", "Invalid platform: $platform"))
                }
            }
            
            errors
        }
    }
    
    override fun describeParameters(params: Map<String, String>): String {
        val platform = params["webhook.platform"] ?: "Unknown"
        val url = params["webhook.url"] ?: "No URL"
        val triggers = mutableListOf<String>()
        
        if (params["webhook.onStart"]?.toBoolean() == true) triggers.add("Start")
        if (params["webhook.onSuccess"]?.toBoolean() == true) triggers.add("Success")
        if (params["webhook.onFailure"]?.toBoolean() == true) triggers.add("Failure")
        if (params["webhook.onStall"]?.toBoolean() == true) triggers.add("Stall")
        if (params["webhook.onFirstFailure"]?.toBoolean() == true) triggers.add("First Failure")
        if (params["webhook.onBuildFixed"]?.toBoolean() == true) triggers.add("Fixed")
        
        val triggerText = if (triggers.isNotEmpty()) triggers.joinToString(", ") else "No triggers"
        
        return "$platform webhook: $url (Triggers: $triggerText)"
    }
    
    override fun getDefaultParameters(): Map<String, String> {
        return mapOf(
            "webhook.enabled" to "true",
            "webhook.onSuccess" to "false",
            "webhook.onFailure" to "true",
            "webhook.onStart" to "false",
            "webhook.onStall" to "false",
            "webhook.onFirstFailure" to "false",
            "webhook.onBuildFixed" to "false",
            "webhook.buildLongerThanAverage" to "false"
        )
    }
}