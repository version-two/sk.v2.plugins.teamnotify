package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.model.WebhookWithSource
import sk.v2.plugins.teamnotify.model.WebhookSource
import sk.v2.plugins.teamnotify.settings.DisabledWebhooksSettings

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

    private val SETTINGS_KEY = "team-notify.settings"

    fun getWebhooks(project: SProject): List<WebhookConfiguration> {
        return try {
            val settings = projectSettingsManager.getSettings(project.projectId, SETTINGS_KEY) as TeamNotifyProjectSettings
            settings.webhooks
        } catch (e: Exception) {
            // If settings can't be loaded (e.g., during plugin update), return empty list
            emptyList()
        }
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
            onCancel = params["webhook.onCancel"]?.toBoolean() ?: false,
            onFirstFailure = params["webhook.onFirstFailure"]?.toBoolean() ?: false,
            onBuildFixed = params["webhook.onBuildFixed"]?.toBoolean() ?: false,
            buildLongerThanAverage = params["webhook.buildLongerThanAverage"]?.toBoolean() ?: false,
            buildLongerThan = params["webhook.buildLongerThan"]?.toIntOrNull(),
            includeChanges = params["webhook.includeChanges"]?.toBoolean() ?: true,
            branchFilter = params["webhook.branchFilter"]
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
            !buildTypeId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                    ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)
                    
                if (buildType != null) {
                    // Only return build-type specific webhooks, not inherited ones
                    try {
                        val buildTypeKey = "${SETTINGS_KEY}.${buildType.buildTypeId}"
                        val settings = projectSettingsManager.getSettings(buildType.project.projectId, buildTypeKey) as TeamNotifyProjectSettings
                        settings.webhooks
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
            !projectId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val project = sBuildServer.projectManager.findProjectByExternalId(projectId)
                    ?: sBuildServer.projectManager.findProjectById(projectId)
                    
                project?.let { getWebhooks(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    fun saveWebhooksForEntity(projectId: String?, buildTypeId: String?, webhooks: List<WebhookConfiguration>) {
        when {
            !buildTypeId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                    ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)
                    
                if (buildType != null) {
                    saveWebhooksForBuildType(buildType, webhooks)
                } else {
                    throw IllegalArgumentException("Build type not found: $buildTypeId")
                }
            }
            !projectId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val project = sBuildServer.projectManager.findProjectByExternalId(projectId)
                    ?: sBuildServer.projectManager.findProjectById(projectId)
                    
                if (project != null) {
                    saveWebhooks(project, webhooks)
                } else {
                    throw IllegalArgumentException("Project not found: $projectId")
                }
            }
            else -> {
                throw IllegalArgumentException("Either projectId or buildTypeId must be provided")
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
    
    // Get locally disabled webhook URLs for a build type
    fun getDisabledWebhooksForBuildType(buildType: SBuildType): Set<String> {
        val disabledKey = "${SETTINGS_KEY}.disabled.${buildType.buildTypeId}"
        return try {
            val settings = projectSettingsManager.getSettings(buildType.project.projectId, disabledKey) as DisabledWebhooksSettings
            settings.disabledWebhookUrls
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    // Save locally disabled webhook URLs for a build type
    fun saveDisabledWebhooksForBuildType(buildType: SBuildType, disabledUrls: Set<String>) {
        val disabledKey = "${SETTINGS_KEY}.disabled.${buildType.buildTypeId}"
        val settings = projectSettingsManager.getSettings(buildType.project.projectId, disabledKey) as DisabledWebhooksSettings
        settings.disabledWebhookUrls.clear()
        settings.disabledWebhookUrls.addAll(disabledUrls)
        buildType.project.persist()
    }
    
    // Get webhooks with source information for display in build configuration
    fun getWebhooksWithSourceForBuildType(buildType: SBuildType): List<WebhookWithSource> {
        val webhooksMap = mutableMapOf<String, WebhookWithSource>()
        val disabledUrls = getDisabledWebhooksForBuildType(buildType)
        
        // Start with project webhooks (lowest priority)
        val projectWebhooks = getWebhooksIncludingParents(buildType.project)
        projectWebhooks.forEach { webhook ->
            webhooksMap[webhook.url] = WebhookWithSource(
                webhook = webhook,
                source = WebhookSource.PROJECT,
                isLocallyDisabled = disabledUrls.contains(webhook.url)
            )
        }
        
        // Then add build-type specific webhooks (higher priority - overwrites project webhooks with same URL)
        val buildTypeWebhooks = try {
            val buildTypeKey = "${SETTINGS_KEY}.${buildType.buildTypeId}"
            val settings = projectSettingsManager.getSettings(buildType.project.projectId, buildTypeKey) as TeamNotifyProjectSettings
            settings.webhooks
        } catch (e: Exception) {
            emptyList()
        }
        buildTypeWebhooks.forEach { webhook ->
            webhooksMap[webhook.url] = WebhookWithSource(
                webhook = webhook,
                source = WebhookSource.BUILD_TYPE,
                isLocallyDisabled = false // Build-type specific webhooks can't be locally disabled, they're just deleted
            )
        }
        
        // Finally add DSL-defined features (highest priority - overwrites all others)
        val dslWebhooks = getDslWebhooksForBuildType(buildType)
        dslWebhooks.forEach { webhook ->
            webhooksMap[webhook.url] = WebhookWithSource(
                webhook = webhook,
                source = WebhookSource.DSL,
                isLocallyDisabled = disabledUrls.contains(webhook.url)
            )
        }
        
        return webhooksMap.values.toList()
    }
    
    // Get effective webhooks for build type (for sending notifications)
    fun getEffectiveWebhooksForBuildType(buildType: SBuildType): List<WebhookConfiguration> {
        val webhooksWithSource = getWebhooksWithSourceForBuildType(buildType)
        return webhooksWithSource
            .filter { !it.isLocallyDisabled && it.webhook.enabled }
            .map { it.webhook }
    }
}