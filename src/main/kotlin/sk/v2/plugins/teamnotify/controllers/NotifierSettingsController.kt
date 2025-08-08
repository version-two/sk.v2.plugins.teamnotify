package sk.v2.plugins.teamnotify.controllers

import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.services.WebhookManager
import sk.v2.plugins.teamnotify.services.WebhookService
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import jetbrains.buildServer.serverSide.SBuildServer
import java.net.URLEncoder

class NotifierSettingsController(
    private val webControllerManager: WebControllerManager,
    private val pluginDescriptor: PluginDescriptor,
    private val webhookManager: WebhookManager,
    private val webhookService: WebhookService,
    private val sBuildServer: SBuildServer
) : BaseController() {

    fun register(): Unit {
        webControllerManager.registerController("/notifier/settings.html", this)
        webControllerManager.registerController("/notifier/testWebhook.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val path = request.requestURI ?: ""
        if (path.endsWith("/notifier/testWebhook.html")) {
            val webhookUrl = request.getParameter("webhookUrl")?.trim()
            val platformRaw = request.getParameter("platform")?.trim()?.uppercase()
            val platform = try {
                WebhookPlatform.valueOf(platformRaw ?: "")
            } catch (e: Exception) { null }

            response.contentType = "application/json; charset=utf-8"
            if (webhookUrl.isNullOrBlank() || platform == null || !isValidWebhookUrl(platform, webhookUrl)) {
                response.status = 400
                response.writer.write("""{"success":false,"status":400,"message":"Invalid platform or URL"}""")
                return null
            }

            val result = webhookService.testWebhook(webhookUrl, platform)
            val msg = (result.errorBody ?: "").replace("\"", "\\\"")
            response.status = if (result.success) 200 else if (result.statusCode > 0) result.statusCode else 500
            response.writer.write("""{"success":${result.success},"status":${result.statusCode},"message":"$msg"}""")
            return null
        }
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
                val webhookUrl = request.getParameter("webhookUrl")?.trim()
                val platformRaw = request.getParameter("platform")?.trim()?.uppercase()
                val onSuccess = request.getParameter("onSuccess") != null
                val onFailure = request.getParameter("onFailure") != null
                val onStall = request.getParameter("onStall") != null
                val buildLongerThan = request.getParameter("buildLongerThan")?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                val buildLongerThanAverage = request.getParameter("buildLongerThanAverage") != null
                val onFirstFailure = request.getParameter("onFirstFailure") != null
                val onBuildFixed = request.getParameter("onBuildFixed") != null
                val onStart = request.getParameter("onStart") != null

                val errors = mutableListOf<String>()
                val platform = try {
                    WebhookPlatform.valueOf(platformRaw ?: "")
                } catch (e: Exception) {
                    errors += "Invalid platform selected."
                    null
                }

                if (webhookUrl.isNullOrBlank()) {
                    errors += "Webhook URL is required."
                } else if (platform != null && !isValidWebhookUrl(platform, webhookUrl)) {
                    val expected = when (platform) {
                        WebhookPlatform.SLACK -> "https://hooks.slack.com/services/..."
                        WebhookPlatform.TEAMS -> "https://{tenant}.webhook.office.com/webhookb2/... or https://outlook.office.com/..."
                        WebhookPlatform.DISCORD -> "https://discord.com/api/webhooks/{id}/{token}"
                    }
                    errors += "Webhook URL does not match the expected format for $platform. Example: $expected"
                }

                if (buildLongerThan != null && buildLongerThan <= 0) {
                    errors += "Duration threshold must be a positive number of seconds."
                }

                if (errors.isNotEmpty()) {
                    // Re-render form with errors and prefilled values
                    mv.model["validationErrors"] = errors
                    mv.model["formUrl"] = webhookUrl ?: ""
                    mv.model["formPlatform"] = platformRaw ?: "SLACK"
                    mv.model["formOnStart"] = onStart
                    mv.model["formOnSuccess"] = onSuccess
                    mv.model["formOnFailure"] = onFailure
                    mv.model["formOnStall"] = onStall
                    mv.model["formBuildLongerThanAverage"] = buildLongerThanAverage
                    if (buildLongerThan != null) mv.model["formBuildLongerThan"] = buildLongerThan
                } else {
                    val newWebhook = WebhookConfiguration(
                        url = webhookUrl!!,
                        platform = platform!!,
                        onStart = onStart,
                        onSuccess = onSuccess,
                        onFailure = onFailure,
                        onStall = onStall,
                        buildLongerThan = buildLongerThan,
                        buildLongerThanAverage = buildLongerThanAverage,
                        onFirstFailure = onFirstFailure,
                        onBuildFixed = onBuildFixed
                    )
                    val existingWebhooks = webhookManager.getWebhooks(project).toMutableList()
                    existingWebhooks.add(newWebhook)
                    webhookManager.saveWebhooks(project, existingWebhooks)

                    val message = "Webhook added successfully!"
                    val back = preferredReturnUrl(request)
                    if (back != null) {
                        val sep = if (back.contains("?")) "&" else "?"
                        val enc = URLEncoder.encode(message, "UTF-8")
                        return ModelAndView("redirect:" + back + sep + "message=" + enc)
                    } else {
                        val redirectUrl = if (buildTypeId != null) {
                            "redirect:/notifier/settings.html?buildTypeId=$buildTypeId&message=$message"
                        } else {
                            "redirect:/notifier/settings.html?projectId=$projectId&message=$message"
                        }
                        return ModelAndView(redirectUrl)
                    }
                }
            } else if (action == "delete") {
                val webhookUrlToDelete = request.getParameter("webhookUrlToDelete")
                val existingWebhooks = webhookManager.getWebhooks(project).toMutableList()
                existingWebhooks.removeIf { it.url == webhookUrlToDelete }
                webhookManager.saveWebhooks(project, existingWebhooks)

                val message = "Webhook deleted successfully!"
                val back = preferredReturnUrl(request)
                if (back != null) {
                    val sep = if (back.contains("?")) "&" else "?"
                    val enc = URLEncoder.encode(message, "UTF-8")
                    return ModelAndView("redirect:" + back + sep + "message=" + enc)
                } else {
                    val redirectUrl = if (buildTypeId != null) {
                        "redirect:/notifier/settings.html?buildTypeId=$buildTypeId&message=$message"
                    } else {
                        "redirect:/notifier/settings.html?projectId=$projectId&message=$message"
                    }
                    return ModelAndView(redirectUrl)
                }
            }
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

    private fun preferredReturnUrl(request: HttpServletRequest): String? {
        val ref = request.getHeader("Referer")
        val ret = request.getParameter("returnUrl")
        val page = request.getParameter("pageUrl")
        val candidates = listOf(ref, ret, page).filterNotNull()
        return candidates.firstOrNull { url ->
            url.contains("/project", ignoreCase = true) ||
            url.contains("projectSettings", ignoreCase = true) ||
            url.contains("/buildType", ignoreCase = true) ||
            url.contains("editBuildType", ignoreCase = true)
        }
    }

    private fun isValidWebhookUrl(platform: WebhookPlatform, url: String): Boolean {
        return when (platform) {
            WebhookPlatform.SLACK -> Regex("^https://hooks\\.slack\\.com/.*").matches(url)
            WebhookPlatform.TEAMS -> Regex("^(https://.*webhook\\.office\\.com/|https://outlook\\.office\\.com/).*").matches(url)
            WebhookPlatform.DISCORD -> Regex("^https://discord(?:app)?\\.com/api/webhooks/.*").matches(url)
        }
    }
}