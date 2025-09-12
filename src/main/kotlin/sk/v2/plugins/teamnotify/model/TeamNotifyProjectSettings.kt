package sk.v2.plugins.teamnotify.model

import jetbrains.buildServer.serverSide.settings.ProjectSettings
import org.jdom.Element

data class TeamNotifyProjectSettings(
    val webhooks: MutableList<WebhookConfiguration> = mutableListOf()
) : ProjectSettings {
    
    companion object {
        // This ID must remain stable across plugin versions
        const val SETTINGS_ID = "team-notify-webhooks"
    }

    override fun readFrom(parentElement: Element) {
        webhooks.clear()
        
        val webhooksElement = parentElement.getChild("webhooks") ?: return
        val webhookElements = webhooksElement.getChildren("webhook")
        
        for (webhookObj in webhookElements) {
            val webhookElement = webhookObj as? Element ?: continue
            try {
                val url = webhookElement.getChild("url")?.textTrim ?: continue
                val platformText = webhookElement.getChild("platform")?.textTrim ?: continue
                val platform = try { 
                    WebhookPlatform.valueOf(platformText) 
                } catch (e: Exception) { 
                    continue 
                }
                
                val webhook = WebhookConfiguration(
                    url = url,
                    platform = platform,
                    enabled = webhookElement.getChild("enabled")?.textTrim?.toBoolean() ?: true,
                    onStart = webhookElement.getChild("onStart")?.textTrim?.toBoolean() ?: false,
                    onSuccess = webhookElement.getChild("onSuccess")?.textTrim?.toBoolean() ?: false,
                    onFailure = webhookElement.getChild("onFailure")?.textTrim?.toBoolean() ?: false,
                    onStall = webhookElement.getChild("onStall")?.textTrim?.toBoolean() ?: false,
                    onCancel = webhookElement.getChild("onCancel")?.textTrim?.toBoolean() ?: false,
                    onFirstFailure = webhookElement.getChild("onFirstFailure")?.textTrim?.toBoolean() ?: false,
                    onBuildFixed = webhookElement.getChild("onBuildFixed")?.textTrim?.toBoolean() ?: false,
                    buildLongerThanAverage = webhookElement.getChild("buildLongerThanAverage")?.textTrim?.toBoolean() ?: false,
                    buildLongerThan = webhookElement.getChild("buildLongerThan")?.textTrim?.toIntOrNull(),
                    includeChanges = webhookElement.getChild("includeChanges")?.textTrim?.toBoolean() ?: true,
                    branchFilter = webhookElement.getChild("branchFilter")?.textTrim
                )
                webhooks.add(webhook)
            } catch (e: Exception) {
                // Skip malformed webhook entries
            }
        }
    }

    override fun writeTo(parentElement: Element) {
        val webhooksElement = Element("webhooks")
        parentElement.addContent(webhooksElement)
        
        for (webhook in webhooks) {
            val webhookElement = Element("webhook")
            webhooksElement.addContent(webhookElement)
            
            webhookElement.addContent(Element("url").setText(webhook.url))
            webhookElement.addContent(Element("platform").setText(webhook.platform.name))
            webhookElement.addContent(Element("enabled").setText(webhook.enabled.toString()))
            webhookElement.addContent(Element("onStart").setText(webhook.onStart.toString()))
            webhookElement.addContent(Element("onSuccess").setText(webhook.onSuccess.toString()))
            webhookElement.addContent(Element("onFailure").setText(webhook.onFailure.toString()))
            webhookElement.addContent(Element("onStall").setText(webhook.onStall.toString()))
            webhookElement.addContent(Element("onCancel").setText(webhook.onCancel.toString()))
            webhookElement.addContent(Element("onFirstFailure").setText(webhook.onFirstFailure.toString()))
            webhookElement.addContent(Element("onBuildFixed").setText(webhook.onBuildFixed.toString()))
            webhookElement.addContent(Element("buildLongerThanAverage").setText(webhook.buildLongerThanAverage.toString()))
            webhook.buildLongerThan?.let {
                webhookElement.addContent(Element("buildLongerThan").setText(it.toString()))
            }
            webhookElement.addContent(Element("includeChanges").setText(webhook.includeChanges.toString()))
            webhook.branchFilter?.let {
                webhookElement.addContent(Element("branchFilter").setText(it))
            }
        }
    }

    override fun dispose() {
        webhooks.clear()
    }
}