package sk.v2.plugins.teamnotify.controllers

import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.services.WebhookManager
import sk.v2.plugins.teamnotify.services.WebhookService
import sk.v2.plugins.teamnotify.utils.BranchMatcher
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
        webControllerManager.registerController("/notifier/api/webhooks.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val path = request.requestURI ?: ""
        
        // Handle API requests for AJAX operations
        if (path.endsWith("/notifier/api/webhooks.html")) {
            return handleApiRequest(request, response)
        }
        
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
                val onCancel = request.getParameter("onCancel") != null
                val buildLongerThan = request.getParameter("buildLongerThan")?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                val buildLongerThanAverage = request.getParameter("buildLongerThanAverage") != null
                val onFirstFailure = request.getParameter("onFirstFailure") != null
                val onBuildFixed = request.getParameter("onBuildFixed") != null
                val onStart = request.getParameter("onStart") != null
                val includeChanges = request.getParameter("includeChanges") != null
                val branchFilter = request.getParameter("branchFilter")?.trim()?.takeIf { it.isNotEmpty() }

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
                
                if (branchFilter != null && !BranchMatcher.isValidPattern(branchFilter)) {
                    errors += "Invalid branch filter pattern."
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
                        onCancel = onCancel,
                        buildLongerThan = buildLongerThan,
                        buildLongerThanAverage = buildLongerThanAverage,
                        onFirstFailure = onFirstFailure,
                        onBuildFixed = onBuildFixed,
                        includeChanges = includeChanges,
                        branchFilter = branchFilter
                    )
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                    existingWebhooks.add(newWebhook)
                    webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)

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
                val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                existingWebhooks.removeIf { it.url == webhookUrlToDelete }
                webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)

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
            // If we're in a build configuration, get webhooks with source info
            if (buildTypeId != null) {
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                if (buildType != null) {
                    mv.model["webhooksWithSource"] = webhookManager.getWebhooksWithSourceForBuildType(buildType)
                    mv.model["buildTypeId"] = buildTypeId
                }
            } else {
                // For projects, just get regular webhooks
                mv.model["webhooks"] = webhookManager.getWebhooksForEntity(projectId, buildTypeId)
            }
            mv.model["projectId"] = project.externalId
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
    
    private fun handleApiRequest(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        response.contentType = "application/json; charset=utf-8"
        
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")
        
        // Validate that we have at least one valid entity
        // Priority: buildTypeId > projectId (if both are provided, use buildTypeId)
        val validEntity = when {
            !buildTypeId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val bt = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                    ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)
                bt != null
            }
            !projectId.isNullOrBlank() -> {
                // Try finding by external ID first, then by internal ID
                val proj = sBuildServer.projectManager.findProjectByExternalId(projectId)
                    ?: sBuildServer.projectManager.findProjectById(projectId)
                proj != null
            }
            else -> false
        }
        
        if (!validEntity) {
            response.status = 400
            val errorMsg = when {
                !buildTypeId.isNullOrBlank() -> "Build Configuration not found: $buildTypeId"
                !projectId.isNullOrBlank() -> "Project not found: $projectId"
                else -> "Project or Build Configuration not found"
            }
            response.writer.write("""{"success":false,"error":"$errorMsg"}""")
            return null
        }
        
        when (request.method) {
            "GET" -> {
                // Get all webhooks for the entity (project or build type)
                val webhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId)
                val webhooksJson = webhooks.map { webhook ->
                    """{
                        "url": "${webhook.url.replace("\"", "\\\"")}",
                        "platform": "${webhook.platform}",
                        "onStart": ${webhook.onStart},
                        "onSuccess": ${webhook.onSuccess},
                        "onFailure": ${webhook.onFailure},
                        "onStall": ${webhook.onStall},
                        "buildLongerThan": ${webhook.buildLongerThan ?: "null"},
                        "buildLongerThanAverage": ${webhook.buildLongerThanAverage},
                        "onFirstFailure": ${webhook.onFirstFailure},
                        "onBuildFixed": ${webhook.onBuildFixed},
                        "includeChanges": ${webhook.includeChanges},
                        "branchFilter": ${if (webhook.branchFilter != null) "\"${webhook.branchFilter.replace("\"", "\\\"")}\"" else "null"}
                    }"""
                }.joinToString(",")
                response.writer.write("""{"success":true,"webhooks":[${webhooksJson}]}""")
            }
            "POST" -> {
                // Check if this is a delete action
                val action = request.getParameter("action")
                if (action == "delete") {
                    // Delete a webhook
                    val webhookUrlToDelete = request.getParameter("webhookUrl")
                    if (webhookUrlToDelete.isNullOrBlank()) {
                        response.status = 400
                        response.writer.write("""{"success":false,"error":"Webhook URL is required"}""")
                        return null
                    }
                    
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                    val removed = existingWebhooks.removeIf { it.url == webhookUrlToDelete }
                    
                    if (removed) {
                        webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)
                        response.writer.write("""{"success":true,"message":"Webhook deleted successfully"}""")
                    } else {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                    }
                    return null
                }
                if (action == "toggle") {
                    // Toggle webhook enable/disable
                    val webhookUrl = request.getParameter("webhookUrl")
                    if (webhookUrl.isNullOrBlank()) {
                        response.status = 400
                        response.writer.write("""{"success":false,"error":"Webhook URL is required"}""")
                        return null
                    }
                    
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                    val webhookIndex = existingWebhooks.indexOfFirst { it.url == webhookUrl }
                    
                    if (webhookIndex != -1) {
                        val oldWebhook = existingWebhooks[webhookIndex]
                        val newWebhook = oldWebhook.copy(enabled = !oldWebhook.enabled)
                        existingWebhooks[webhookIndex] = newWebhook
                        webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)
                        response.writer.write("""{"success":true,"enabled":${newWebhook.enabled},"message":"Webhook ${if (newWebhook.enabled) "enabled" else "disabled"} successfully"}""")
                    } else {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Webhook not found"}""")
                    }
                    return null
                }
                if (action == "toggleLocal" && buildTypeId != null) {
                    // Toggle local disable for inherited webhook in build configuration
                    val webhookUrl = request.getParameter("webhookUrl")
                    if (webhookUrl.isNullOrBlank()) {
                        response.status = 400
                        response.writer.write("""{"success":false,"error":"Webhook URL is required"}""")
                        return null
                    }
                    
                    // Try finding by external ID first, then by internal ID
                    val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                        ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)
                    
                    if (buildType != null) {
                        val disabledUrls = webhookManager.getDisabledWebhooksForBuildType(buildType).toMutableSet()
                        val isCurrentlyDisabled = disabledUrls.contains(webhookUrl)
                        
                        if (isCurrentlyDisabled) {
                            disabledUrls.remove(webhookUrl)
                        } else {
                            disabledUrls.add(webhookUrl)
                        }
                        
                        webhookManager.saveDisabledWebhooksForBuildType(buildType, disabledUrls)
                        val newStatus = !isCurrentlyDisabled
                        response.writer.write("""{"success":true,"locallyDisabled":$newStatus,"message":"Webhook ${if (newStatus) "locally disabled" else "locally enabled"} for this build configuration"}""")
                    } else {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Build configuration not found"}""")
                    }
                    return null
                }
                
                // Add a new webhook
                val webhookUrl = request.getParameter("webhookUrl")?.trim()
                val platformRaw = request.getParameter("platform")?.trim()?.uppercase()
                val onSuccess = request.getParameter("onSuccess")?.toBoolean() ?: false
                val onFailure = request.getParameter("onFailure")?.toBoolean() ?: false
                val onStall = request.getParameter("onStall")?.toBoolean() ?: false
                val onCancel = request.getParameter("onCancel")?.toBoolean() ?: false
                val buildLongerThan = request.getParameter("buildLongerThan")?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
                val buildLongerThanAverage = request.getParameter("buildLongerThanAverage")?.toBoolean() ?: false
                val onFirstFailure = request.getParameter("onFirstFailure")?.toBoolean() ?: false
                val onBuildFixed = request.getParameter("onBuildFixed")?.toBoolean() ?: false
                val onStart = request.getParameter("onStart")?.toBoolean() ?: false
                val includeChanges = request.getParameter("includeChanges")?.toBoolean() ?: true
                val branchFilter = request.getParameter("branchFilter")?.trim()?.takeIf { it.isNotEmpty() }
                
                val platform = try {
                    WebhookPlatform.valueOf(platformRaw ?: "")
                } catch (e: Exception) {
                    null
                }
                
                if (webhookUrl.isNullOrBlank() || platform == null || !isValidWebhookUrl(platform, webhookUrl)) {
                    response.status = 400
                    response.writer.write("""{"success":false,"error":"Invalid webhook URL or platform"}""")
                    return null
                }
                
                val newWebhook = WebhookConfiguration(
                    url = webhookUrl,
                    platform = platform,
                    onStart = onStart,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                    onStall = onStall,
                    onCancel = onCancel,
                    buildLongerThan = buildLongerThan,
                    buildLongerThanAverage = buildLongerThanAverage,
                    onFirstFailure = onFirstFailure,
                    onBuildFixed = onBuildFixed,
                    includeChanges = includeChanges,
                    branchFilter = branchFilter
                )
                
                val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                existingWebhooks.add(newWebhook)
                webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)
                
                response.writer.write("""{"success":true,"message":"Webhook added successfully"}""")
            }
            else -> {
                response.status = 405
                response.writer.write("""{"success":false,"error":"Method not allowed"}""")
            }
        }
        
        return null
    }
}