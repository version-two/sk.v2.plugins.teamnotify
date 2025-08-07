package sk.v2.plugins.teamnotify.controllers

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import sk.v2.plugins.teamnotify.services.WebhookManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TeamNotifyAdminController(
    private val webControllerManager: WebControllerManager,
    private val pluginDescriptor: PluginDescriptor,
    private val webhookManager: WebhookManager,
    private val sBuildServer: SBuildServer
) : BaseController() {

    fun register() {
        webControllerManager.registerController("/admin/teamnotify.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("teamNotifyAdmin.jsp"))
        
        // Collect all webhook configurations from all projects
        val allWebhooks = webhookManager.getAllWebhooks()
        
        mv.model["allWebhooks"] = allWebhooks
        mv.model["totalWebhooks"] = allWebhooks.size
        
        return mv
    }
}