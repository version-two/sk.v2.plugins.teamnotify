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
        webControllerManager.registerController("/admin/teamnotify/api.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val path = request.requestURI ?: ""
        
        // Handle API requests
        if (path.endsWith("/api.html")) {
            return handleApiRequest(request, response)
        }
        
        // Handle regular page request
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("teamNotifyAdmin.jsp"))
        
        // Collect all webhook configurations from all projects
        val allWebhooks = webhookManager.getAllWebhooks()
        
        mv.model["allWebhooks"] = allWebhooks
        mv.model["totalWebhooks"] = allWebhooks.size
        
        return mv
    }
    
    private fun handleApiRequest(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        response.contentType = "application/json; charset=utf-8"
        
        val action = request.getParameter("action")
        val webhookUrl = request.getParameter("webhookUrl")
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")
        
        when (action) {
            "delete" -> {
                if (webhookUrl.isNullOrBlank()) {
                    response.status = 400
                    response.writer.write("""{"success":false,"error":"Webhook URL is required"}""")
                    return null
                }
                
                val webhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                val removed = webhooks.removeIf { it.url == webhookUrl }
                
                if (removed) {
                    webhookManager.saveWebhooksForEntity(projectId, buildTypeId, webhooks)
                    response.writer.write("""{"success":true,"message":"Webhook deleted successfully"}""")
                } else {
                    response.status = 404
                    response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                }
            }
            "toggle" -> {
                if (webhookUrl.isNullOrBlank()) {
                    response.status = 400
                    response.writer.write("""{"success":false,"error":"Webhook URL is required"}""")
                    return null
                }
                
                val webhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                val webhookIndex = webhooks.indexOfFirst { it.url == webhookUrl }
                
                if (webhookIndex != -1) {
                    val oldWebhook = webhooks[webhookIndex]
                    val newWebhook = oldWebhook.copy(enabled = !oldWebhook.enabled)
                    webhooks[webhookIndex] = newWebhook
                    webhookManager.saveWebhooksForEntity(projectId, buildTypeId, webhooks)
                    response.writer.write("""{"success":true,"enabled":${newWebhook.enabled}}""")
                } else {
                    response.status = 404
                    response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                }
            }
            else -> {
                response.status = 400
                response.writer.write("""{"success":false,"error":"Invalid action"}""")
            }
        }
        
        return null
    }
}