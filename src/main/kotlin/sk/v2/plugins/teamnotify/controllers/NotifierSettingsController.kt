package sk.v2.plugins.teamnotify.controllers

import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.services.WebhookManager
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import jetbrains.buildServer.serverSide.SBuildServer

class NotifierSettingsController(
    private val webControllerManager: WebControllerManager,
    private val pluginDescriptor: PluginDescriptor,
    private val webhookManager: WebhookManager,
    private val sBuildServer: SBuildServer
) : BaseController() {

    fun register(): Unit {
        webControllerManager.registerController("/notifier/settings.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")
        
        // Determine the project - either directly or via build configuration
        val project = when {
            projectId != null -> sBuildServer.projectManager.findProjectByExternalId(projectId)
            buildTypeId != null -> sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)?.project
            else -> null
        }
        
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))

        if (request.method == "POST" && project != null) {
            val action = request.getParameter("action")
            if (action == "add") {
                val webhookUrl = request.getParameter("webhookUrl")
                val platform = request.getParameter("platform")
                val onSuccess = request.getParameter("onSuccess") != null
                val onFailure = request.getParameter("onFailure") != null
                val onStall = request.getParameter("onStall") != null
                val buildLongerThan = request.getParameter("buildLongerThan")?.toIntOrNull()
                val buildLongerThanAverage = request.getParameter("buildLongerThanAverage") != null
                val onFirstFailure = request.getParameter("onFirstFailure") != null
                val onBuildFixed = request.getParameter("onBuildFixed") != null

                val newWebhook = WebhookConfiguration(webhookUrl, WebhookPlatform.valueOf(platform?.toUpperCase() ?: "SLACK"), onSuccess, onFailure, onStall, buildLongerThan, buildLongerThanAverage, onFirstFailure, onBuildFixed)
                val existingWebhooks = webhookManager.getWebhooks(project).toMutableList()
                existingWebhooks.add(newWebhook)
                webhookManager.saveWebhooks(project, existingWebhooks)
            } else if (action == "delete") {
                val webhookUrlToDelete = request.getParameter("webhookUrlToDelete")
                val existingWebhooks = webhookManager.getWebhooks(project).toMutableList()
                existingWebhooks.removeIf { it.url == webhookUrlToDelete }
                webhookManager.saveWebhooks(project, existingWebhooks)
            }
            val message = if (action == "add") "Webhook added successfully!" else "Webhook deleted successfully!"
            val redirectUrl = if (buildTypeId != null) {
                "redirect:/notifier/settings.html?buildTypeId=$buildTypeId&message=$message"
            } else {
                "redirect:/notifier/settings.html?projectId=$projectId&message=$message"
            }
            return ModelAndView(redirectUrl)
        }

        if (project != null) {
            mv.model["webhooks"] = webhookManager.getWebhooks(project)
            mv.model["projectId"] = project.externalId
            if (buildTypeId != null) {
                mv.model["buildTypeId"] = buildTypeId
            }
        }
        return mv
    }
}