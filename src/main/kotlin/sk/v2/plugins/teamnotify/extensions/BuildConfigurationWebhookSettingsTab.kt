package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.DebugLogger
import sk.v2.plugins.teamnotify.services.WebhookManager
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
        LOG.warn("BuildConfigurationWebhookSettingsTab is being initialized")
        DebugLogger.log("BuildConfigurationWebhookSettingsTab is being initialized")
        
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
        
        LOG.warn("BuildConfigurationWebhookSettingsTab initialization complete")
        DebugLogger.log("BuildConfigurationWebhookSettingsTab initialization complete")
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest, buildType: SBuildType, user: SUser?) {
        LOG.warn("BuildConfigurationWebhookSettingsTab.fillModel called for buildType: ${buildType.name}")
        DebugLogger.log("BuildConfigurationWebhookSettingsTab.fillModel called for buildType: ${buildType.name}")
        
        model["projectId"] = buildType.project.externalId
        model["buildTypeId"] = buildType.externalId
        model["project"] = buildType.project
        model["buildType"] = buildType
        try {
            val hooks = webhookManager.getWebhooks(buildType.project)
            model["webhooks"] = hooks
        } catch (e: Exception) {
            LOG.warn("Failed to load webhooks for buildType ${buildType.externalId}: ${e.message}")
        }
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        LOG.warn("BuildConfigurationWebhookSettingsTab.isAvailable called")
        DebugLogger.log("BuildConfigurationWebhookSettingsTab.isAvailable called")
        return true
    }
}