package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings
import sk.v2.plugins.teamnotify.model.WebhookConfiguration

data class WebhookWithProjectInfo(
    val webhook: WebhookConfiguration,
    val projectName: String,
    val projectId: String,
    val buildTypeName: String? = null,
    val buildTypeId: String? = null
)

class WebhookManager(
    private val projectSettingsManager: ProjectSettingsManager,
    private val sBuildServer: SBuildServer
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

    fun getAllWebhooks(): List<WebhookWithProjectInfo> {
        val allWebhooks = mutableListOf<WebhookWithProjectInfo>()
        
        // Get all projects from the server
        val allProjects = sBuildServer.projectManager.projects
        
        for (project in allProjects) {
            try {
                val webhooks = getWebhooks(project)
                for (webhook in webhooks) {
                    allWebhooks.add(
                        WebhookWithProjectInfo(
                            webhook = webhook,
                            projectName = project.name,
                            projectId = project.externalId
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip projects that don't have webhook settings or have errors
                continue
            }
        }
        
        return allWebhooks
    }
}