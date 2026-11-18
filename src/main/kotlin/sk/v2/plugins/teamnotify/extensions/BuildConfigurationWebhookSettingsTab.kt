package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.services.WebhookManager
import sk.v2.plugins.teamnotify.utils.VersionedSettings
import javax.servlet.http.HttpServletRequest

class BuildConfigurationWebhookSettingsTab(
    @NotNull webControllerManager: WebControllerManager,
    @NotNull projectManager: ProjectManager,
    @NotNull private val pluginDescriptor: PluginDescriptor,
    @NotNull private val webhookManager: WebhookManager
) : BuildTypeTab(
    "webhookNotifier",
    "TeamNotify",
    webControllerManager,
    projectManager
) {
    private val LOG = Logger.getInstance(BuildConfigurationWebhookSettingsTab::class.java.name)

    init {
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest, buildType: SBuildType, user: SUser?) {
        model["projectId"] = buildType.project.externalId
        model["buildTypeId"] = buildType.externalId
        model["project"] = buildType.project
        model["buildType"] = buildType
        
        // Check if versioned settings are enabled and in read-only mode
        val project = buildType.project
        model["versionedSettingsEnabled"] = VersionedSettings.isEnabled(project)
        model["versionedSettingsReadOnly"] = VersionedSettings.isReadOnly(project)
        
        try {
            // Get all webhooks for this build type including inherited ones with source information
            val webhooksWithSource = webhookManager.getWebhooksWithSourceForBuildType(buildType)
            model["webhooksWithSource"] = webhooksWithSource
        } catch (e: Exception) {
            LOG.warn("Failed to load webhooks for buildType ${buildType.externalId}: ${e.message}")
        }
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        return true
    }
}