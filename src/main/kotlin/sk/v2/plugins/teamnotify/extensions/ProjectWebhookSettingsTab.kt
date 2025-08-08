package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.openapi.project.ProjectTab
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.DebugLogger
import javax.servlet.http.HttpServletRequest

class ProjectWebhookSettingsTab(
    @NotNull webControllerManager: WebControllerManager,
    @NotNull projectManager: ProjectManager,
    @NotNull private val pluginDescriptor: PluginDescriptor
) : ProjectTab(
    "webhookNotifier", 
    "Webhook Notifications",
    webControllerManager,
    projectManager
) {
    private val LOG = Logger.getInstance(ProjectWebhookSettingsTab::class.java.name)

    init {
        LOG.warn("ProjectWebhookSettingsTab is being initialized")
        DebugLogger.log("ProjectWebhookSettingsTab is being initialized")
        
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
        
        LOG.warn("ProjectWebhookSettingsTab initialization complete")
        DebugLogger.log("ProjectWebhookSettingsTab initialization complete")
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest, project: SProject, user: SUser?) {
        LOG.warn("ProjectWebhookSettingsTab.fillModel called for project: ${project.name}")
        DebugLogger.log("ProjectWebhookSettingsTab.fillModel called for project: ${project.name}")
        
        model["projectId"] = project.externalId
        model["project"] = project
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        LOG.warn("ProjectWebhookSettingsTab.isAvailable called")
        DebugLogger.log("ProjectWebhookSettingsTab.isAvailable called")
        return true
    }
}