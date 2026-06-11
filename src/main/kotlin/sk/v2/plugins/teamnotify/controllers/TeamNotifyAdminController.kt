package sk.v2.plugins.teamnotify.controllers

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.util.SessionUser
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
        // The admin overview lists every webhook across all projects and can delete/toggle them,
        // so it is restricted to server administrators.
        val user = SessionUser.getUser(request)
        if (user == null || !user.isPermissionGrantedGlobally(Permission.CHANGE_SERVER_SETTINGS)) {
            response.status = 403
            response.contentType = "application/json; charset=utf-8"
            response.writer.write("""{"success":false,"error":"Forbidden: server administrator permission required"}""")
            return null
        }

        val path = request.requestURI ?: ""

        if (path.endsWith("/api.html")) {
            return handleApiRequest(request, response)
        }

        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("teamNotifyAdmin.jsp"))
        val allWebhooks = webhookManager.getAllWebhooks()
        
        mv.model["allWebhooks"] = allWebhooks
        mv.model["totalWebhooks"] = allWebhooks.size
        
        return mv
    }
    
    private fun handleApiRequest(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        response.contentType = "application/json; charset=utf-8"

        val action = request.getParameter("action")
        val webhookIndexStr = request.getParameter("webhookIndex")
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")
        val allWebhooks = webhookManager.getAllWebhooks()

        val webhookIndex = webhookIndexStr?.toIntOrNull()
        if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= allWebhooks.size) {
            response.status = 400
            response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
            return null
        }

        val targetWebhook = allWebhooks[webhookIndex]
        val webhookUrl = targetWebhook.webhook.url
        val effectiveProjectId = if (projectId.isNullOrBlank() || projectId == "null") targetWebhook.projectId else projectId
        val effectiveBuildTypeId = if (buildTypeId.isNullOrBlank() || buildTypeId == "null") targetWebhook.buildTypeId else buildTypeId

        when (action) {
            "delete" -> {
                try {
                    val webhooks = webhookManager.getWebhooksForEntity(effectiveProjectId, effectiveBuildTypeId).toMutableList()
                    val removed = webhooks.removeIf { it.url == webhookUrl }

                    if (removed) {
                        webhookManager.saveWebhooksForEntity(effectiveProjectId, effectiveBuildTypeId, webhooks)
                        response.writer.write("""{"success":true,"message":"Webhook deleted successfully"}""")
                    } else {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                    }
                } catch (e: Exception) {
                    response.status = 500
                    val errorMsg = e.message?.let { jsonEscape(it) } ?: "Unknown error"
                    response.writer.write("""{"success":false,"error":"Failed to delete webhook: $errorMsg"}""")
                }
            }
            "toggle" -> {
                try {
                    val webhooks = webhookManager.getWebhooksForEntity(effectiveProjectId, effectiveBuildTypeId).toMutableList()
                    val idx = webhooks.indexOfFirst { it.url == webhookUrl }

                    if (idx != -1) {
                        val oldWebhook = webhooks[idx]
                        val newWebhook = oldWebhook.copy(enabled = !oldWebhook.enabled)
                        webhooks[idx] = newWebhook
                        webhookManager.saveWebhooksForEntity(effectiveProjectId, effectiveBuildTypeId, webhooks)
                        response.writer.write("""{"success":true,"enabled":${newWebhook.enabled}}""")
                    } else {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                    }
                } catch (e: Exception) {
                    response.status = 500
                    val errorMsg = e.message?.let { jsonEscape(it) } ?: "Unknown error"
                    response.writer.write("""{"success":false,"error":"Failed to toggle webhook: $errorMsg"}""")
                }
            }
            else -> {
                response.status = 400
                response.writer.write("""{"success":false,"error":"Invalid action"}""")
            }
        }

        return null
    }

    private fun jsonEscape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}