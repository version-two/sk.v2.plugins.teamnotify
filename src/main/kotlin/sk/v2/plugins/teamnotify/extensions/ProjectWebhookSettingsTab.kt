package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.openapi.project.ProjectTab
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.services.WebhookManager
import javax.servlet.http.HttpServletRequest

class ProjectWebhookSettingsTab(
    @NotNull webControllerManager: WebControllerManager,
    @NotNull projectManager: ProjectManager,
    @NotNull private val pluginDescriptor: PluginDescriptor,
    @NotNull private val webhookManager: WebhookManager
) : ProjectTab(
    "webhookNotifier", 
    "TeamNotify",
    webControllerManager,
    projectManager
) {
    private val LOG = Logger.getInstance(ProjectWebhookSettingsTab::class.java.name)

    init {
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest, project: SProject, user: SUser?) {
        model["projectId"] = project.externalId
        model["project"] = project
        try {
            val hooks = webhookManager.getWebhooks(project)
            model["webhooks"] = hooks
        } catch (e: Exception) {
            LOG.warn("Failed to load webhooks for project ${project.externalId}: ${e.message}")
        }
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        return true
    }
}