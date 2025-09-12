package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform

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
    
    fun getWebhooksForBuildType(buildType: SBuildType): List<WebhookConfiguration> {
        val allWebhooks = mutableListOf<WebhookConfiguration>()
        
        // First get webhooks from DSL-defined features (versioned settings)
        val dslWebhooks = getDslWebhooksForBuildType(buildType)
        allWebhooks.addAll(dslWebhooks)
        
        // Then get webhooks specific to this build configuration from UI
        val buildTypeWebhooks = try {
            val buildTypeKey = "${SETTINGS_KEY}.${buildType.buildTypeId}"
            val settings = projectSettingsManager.getSettings(buildType.project.projectId, buildTypeKey) as TeamNotifyProjectSettings
            settings.webhooks
        } catch (e: Exception) {
            emptyList()
        }
        allWebhooks.addAll(buildTypeWebhooks)
        
        // Then get webhooks from the project and all parent projects
        val projectWebhooks = getWebhooksIncludingParents(buildType.project)
        allWebhooks.addAll(projectWebhooks)
        
        // Remove duplicates based on URL
        return allWebhooks.distinctBy { it.url }
    }
    
    fun getWebhooksIncludingParents(project: SProject): List<WebhookConfiguration> {
        val allWebhooks = mutableListOf<WebhookConfiguration>()
        var currentProject: SProject? = project
        
        // Traverse up the project hierarchy to collect webhooks from all parent projects
        while (currentProject != null) {
            // Get DSL-defined webhooks for the project
            val dslWebhooks = getDslWebhooksForProject(currentProject)
            allWebhooks.addAll(dslWebhooks)
            
            // Get UI-defined webhooks
            try {
                val settings = projectSettingsManager.getSettings(currentProject.projectId, SETTINGS_KEY) as TeamNotifyProjectSettings
                allWebhooks.addAll(settings.webhooks)
            } catch (e: Exception) {
                // Skip projects that don't have webhook settings
            }
            currentProject = currentProject.parentProject
        }
        
        return allWebhooks.distinctBy { it.url }
    }
    
    private fun getDslWebhooksForBuildType(buildType: SBuildType): List<WebhookConfiguration> {
        val webhooks = mutableListOf<WebhookConfiguration>()
        
        // Get features defined in the build type through DSL
        buildType.getBuildFeaturesOfType("teamnotify.webhook").forEach { feature ->
            val webhook = parseFeatureToWebhook(feature.parameters)
            webhook?.let { webhooks.add(it) }
        }
        
        return webhooks
    }
    
    private fun getDslWebhooksForProject(project: SProject): List<WebhookConfiguration> {
        val webhooks = mutableListOf<WebhookConfiguration>()
        
        // Get features defined in the project through DSL
        project.getOwnFeaturesOfType("teamnotify.webhook").forEach { feature ->
            val webhook = parseFeatureToWebhook(feature.parameters)
            webhook?.let { webhooks.add(it) }
        }
        
        return webhooks
    }
    
    private fun parseFeatureToWebhook(params: Map<String, String>): WebhookConfiguration? {
        val url = params["webhook.url"] ?: return null
        val platformStr = params["webhook.platform"] ?: return null
        
        val platform = try {
            WebhookPlatform.valueOf(platformStr)
        } catch (e: Exception) {
            return null
        }
        
        return WebhookConfiguration(
            url = url,
            platform = platform,
            enabled = params["webhook.enabled"]?.toBoolean() ?: true,
            onStart = params["webhook.onStart"]?.toBoolean() ?: false,
            onSuccess = params["webhook.onSuccess"]?.toBoolean() ?: false,
            onFailure = params["webhook.onFailure"]?.toBoolean() ?: false,
            onStall = params["webhook.onStall"]?.toBoolean() ?: false,
            onFirstFailure = params["webhook.onFirstFailure"]?.toBoolean() ?: false,
            onBuildFixed = params["webhook.onBuildFixed"]?.toBoolean() ?: false,
            buildLongerThanAverage = params["webhook.buildLongerThanAverage"]?.toBoolean() ?: false,
            buildLongerThan = params["webhook.buildLongerThan"]?.toIntOrNull(),
            includeChanges = params["webhook.includeChanges"]?.toBoolean() ?: true
        )
    }

    fun saveWebhooks(project: SProject, webhooks: List<WebhookConfiguration>) {
        val settings = projectSettingsManager.getSettings(project.projectId, SETTINGS_KEY) as TeamNotifyProjectSettings
        settings.webhooks.clear()
        settings.webhooks.addAll(webhooks)
        project.persist()
    }
    
    fun saveWebhooksForBuildType(buildType: SBuildType, webhooks: List<WebhookConfiguration>) {
        val buildTypeKey = "${SETTINGS_KEY}.${buildType.buildTypeId}"
        val settings = projectSettingsManager.getSettings(buildType.project.projectId, buildTypeKey) as TeamNotifyProjectSettings
        settings.webhooks.clear()
        settings.webhooks.addAll(webhooks)
        buildType.project.persist()
    }
    
    fun getWebhooksForEntity(projectId: String?, buildTypeId: String?): List<WebhookConfiguration> {
        return when {
            buildTypeId != null -> {
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                buildType?.let { getWebhooksForBuildType(it) } ?: emptyList()
            }
            projectId != null -> {
                val project = sBuildServer.projectManager.findProjectByExternalId(projectId)
                project?.let { getWebhooks(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    fun saveWebhooksForEntity(projectId: String?, buildTypeId: String?, webhooks: List<WebhookConfiguration>) {
        when {
            buildTypeId != null -> {
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                buildType?.let { saveWebhooksForBuildType(it, webhooks) }
            }
            projectId != null -> {
                val project = sBuildServer.projectManager.findProjectByExternalId(projectId)
                project?.let { saveWebhooks(it, webhooks) }
            }
        }
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