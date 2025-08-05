package sk.v2.plugins.teamnotify.model

import jetbrains.buildServer.serverSide.settings.ProjectSettings
import org.jdom.Element

data class TeamNotifyProjectSettings(
    val webhooks: MutableList<WebhookConfiguration> = mutableListOf()
) : ProjectSettings {

    override fun readFrom(parentElement: Element) {
        // TeamCity uses bean serialization, so we don't need to implement this manually.
    }

    override fun writeTo(parentElement: Element) {
        // TeamCity uses bean serialization, so we don't need to implement this manually.
    }

    override fun dispose() {
        // No-op
    }
}