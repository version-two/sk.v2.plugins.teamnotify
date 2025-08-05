package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings
import sk.v2.plugins.teamnotify.model.WebhookConfiguration

class WebhookManager(
    private val projectSettingsManager: ProjectSettingsManager
) {

    private val SETTINGS_KEY = "teamnotify.settings"

    fun getWebhooks(project: SProject): List<WebhookConfiguration> {
        val settings = projectSettingsManager.getSettings(project.projectId, SETTINGS_KEY) as TeamNotifyProjectSettings
        return settings.webhooks
    }

    fun saveWebhooks(project: SProject, webhooks: List<WebhookConfiguration>) {
        val settings = projectSettingsManager.getSettings(project.projectId, SETTINGS_KEY) as TeamNotifyProjectSettings
        settings.webhooks.clear()
        settings.webhooks.addAll(webhooks)
        project.persist()
    }
}