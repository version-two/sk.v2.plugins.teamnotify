package sk.v2.plugins.teamnotify.model

import jetbrains.buildServer.serverSide.settings.ProjectSettings
import org.jdom.Element

/**
 * Single project-scoped settings object that holds everything TeamNotify persists for a project:
 *
 *  - [webhooks]            project-level webhooks
 *  - [buildTypeWebhooks]   build-config-level webhooks, keyed by build type internal id (e.g. "bt11")
 *  - [disabledByBuildType] URLs of inherited (project/parent) webhooks that a build config has locally
 *                          disabled, keyed by build type internal id
 *
 * Using one registered settings key (`team-notify.settings`) for all of these avoids the previous
 * design that registered a new settings factory per build type at request time, which produced
 * "corresponding factory was not registered" failures when saving build-level webhooks.
 */
data class TeamNotifyProjectSettings(
    val webhooks: MutableList<WebhookConfiguration> = mutableListOf(),
    val buildTypeWebhooks: MutableMap<String, MutableList<WebhookConfiguration>> = mutableMapOf(),
    val disabledByBuildType: MutableMap<String, MutableSet<String>> = mutableMapOf()
) : ProjectSettings {

    companion object {
        // This ID must remain stable across plugin versions
        const val SETTINGS_ID = "team-notify-webhooks"
    }

    override fun readFrom(parentElement: Element) {
        webhooks.clear()
        buildTypeWebhooks.clear()
        disabledByBuildType.clear()

        // Project-level webhooks: <webhooks><webhook>...</webhook></webhooks>
        readWebhooksInto(parentElement.getChild("webhooks"), webhooks)

        // Build-config-level webhooks: <buildTypeWebhooks><buildType id="bt11"><webhooks>...</webhooks></buildType></buildTypeWebhooks>
        parentElement.getChild("buildTypeWebhooks")?.getChildren("buildType")?.forEach { node ->
            val element = node as? Element ?: return@forEach
            val id = element.getAttributeValue("id")?.trim().orEmpty()
            if (id.isEmpty()) return@forEach
            val list = mutableListOf<WebhookConfiguration>()
            readWebhooksInto(element.getChild("webhooks"), list)
            if (list.isNotEmpty()) buildTypeWebhooks[id] = list
        }

        // Locally disabled inherited webhooks: <disabledWebhooks><buildType id="bt11"><url>...</url></buildType></disabledWebhooks>
        parentElement.getChild("disabledWebhooks")?.getChildren("buildType")?.forEach { node ->
            val element = node as? Element ?: return@forEach
            val id = element.getAttributeValue("id")?.trim().orEmpty()
            if (id.isEmpty()) return@forEach
            val urls = element.getChildren("url")
                .mapNotNull { (it as? Element)?.textTrim }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (urls.isNotEmpty()) disabledByBuildType[id] = urls
        }
    }

    override fun writeTo(parentElement: Element) {
        val webhooksElement = Element("webhooks")
        parentElement.addContent(webhooksElement)
        webhooks.forEach { webhooksElement.addContent(webhookToElement(it)) }

        if (buildTypeWebhooks.isNotEmpty()) {
            val container = Element("buildTypeWebhooks")
            parentElement.addContent(container)
            buildTypeWebhooks.forEach { (id, list) ->
                val buildTypeElement = Element("buildType").setAttribute("id", id)
                container.addContent(buildTypeElement)
                val listElement = Element("webhooks")
                buildTypeElement.addContent(listElement)
                list.forEach { listElement.addContent(webhookToElement(it)) }
            }
        }

        if (disabledByBuildType.isNotEmpty()) {
            val container = Element("disabledWebhooks")
            parentElement.addContent(container)
            disabledByBuildType.forEach { (id, urls) ->
                val buildTypeElement = Element("buildType").setAttribute("id", id)
                container.addContent(buildTypeElement)
                urls.forEach { buildTypeElement.addContent(Element("url").setText(it)) }
            }
        }
    }

    override fun dispose() {
        webhooks.clear()
        buildTypeWebhooks.clear()
        disabledByBuildType.clear()
    }

    private fun readWebhooksInto(webhooksElement: Element?, target: MutableList<WebhookConfiguration>) {
        if (webhooksElement == null) return
        for (webhookObj in webhooksElement.getChildren("webhook")) {
            val webhookElement = webhookObj as? Element ?: continue
            try {
                val url = webhookElement.getChild("url")?.textTrim ?: continue
                val platformText = webhookElement.getChild("platform")?.textTrim ?: continue
                val platform = try {
                    WebhookPlatform.valueOf(platformText)
                } catch (e: Exception) {
                    continue
                }

                target.add(
                    WebhookConfiguration(
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
                        showBuildLink = webhookElement.getChild("showBuildLink")?.textTrim?.toBoolean() ?: true,
                        showArtifacts = webhookElement.getChild("showArtifacts")?.textTrim?.toBoolean() ?: true,
                        branchFilter = webhookElement.getChild("branchFilter")?.textTrim,
                        authHeaderName = webhookElement.getChild("authHeaderName")?.textTrim,
                        authHeaderValue = webhookElement.getChild("authHeaderValue")?.textTrim
                    )
                )
            } catch (e: Exception) {
                // Skip malformed webhook entries
            }
        }
    }

    private fun webhookToElement(webhook: WebhookConfiguration): Element {
        val webhookElement = Element("webhook")
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
        webhookElement.addContent(Element("showBuildLink").setText(webhook.showBuildLink.toString()))
        webhookElement.addContent(Element("showArtifacts").setText(webhook.showArtifacts.toString()))
        webhook.branchFilter?.let {
            webhookElement.addContent(Element("branchFilter").setText(it))
        }
        webhook.authHeaderName?.let {
            webhookElement.addContent(Element("authHeaderName").setText(it))
        }
        webhook.authHeaderValue?.let {
            webhookElement.addContent(Element("authHeaderValue").setText(it))
        }
        return webhookElement
    }
}
