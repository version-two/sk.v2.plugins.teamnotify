package sk.v2.plugins.teamnotify.controllers

import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.model.WebhookSource
import sk.v2.plugins.teamnotify.services.WebhookManager
import sk.v2.plugins.teamnotify.services.WebhookService
import sk.v2.plugins.teamnotify.utils.BranchMatcher
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.util.SessionUser
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.auth.Permission
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

        if (path.endsWith("/notifier/api/webhooks.html")) {
            return handleApiRequest(request, response)
        }
        
        if (path.endsWith("/notifier/testWebhook.html")) {
            // Testing sends a real HTTP request; require an authenticated user.
            if (SessionUser.getUser(request) == null) {
                response.contentType = "application/json; charset=utf-8"
                response.status = 403
                response.writer.write("""{"success":false,"status":403,"message":"Forbidden"}""")
                return null
            }
            val webhookUrl = request.getParameter("webhookUrl")?.trim()
            val platformRaw = request.getParameter("platform")?.trim()?.uppercase()
            val authHeaderName = request.getParameter("authHeaderName")?.trim()?.takeIf { it.isNotEmpty() }
            val authHeaderValue = request.getParameter("authHeaderValue")?.trim()?.takeIf { it.isNotEmpty() }
            val platform = try {
                WebhookPlatform.valueOf(platformRaw ?: "")
            } catch (e: Exception) { null }

            response.contentType = "application/json; charset=utf-8"
            if (webhookUrl.isNullOrBlank() || platform == null || !isValidWebhookUrl(platform, webhookUrl)) {
                response.status = 400
                response.writer.write("""{"success":false,"status":400,"message":"Invalid platform or URL"}""")
                return null
            }

            val result = webhookService.testWebhook(webhookUrl, platform, authHeaderName, authHeaderValue)
            val msg = (result.errorBody ?: "").replace("\"", "\\\"")
            response.status = if (result.success) 200 else if (result.statusCode > 0) result.statusCode else 500
            response.writer.write("""{"success":${result.success},"status":${result.statusCode},"message":"$msg"}""")
            return null
        }
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")

        val project = when {
            projectId != null -> sBuildServer.projectManager.findProjectByExternalId(projectId)
            buildTypeId != null -> sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)?.project
            else -> null
        }
        
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))

        if (request.method == "POST" && project != null && !canEditProject(request, project)) {
            mv.model["validationErrors"] = listOf("You do not have permission to edit webhooks for this project.")
        } else if (request.method == "POST" && project != null) {
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
                val showBuildLink = request.getParameter("showBuildLink") != null
                val showArtifacts = request.getParameter("showArtifacts") != null
                val branchFilter = request.getParameter("branchFilter")?.trim()?.takeIf { it.isNotEmpty() }
                val authHeaderName = request.getParameter("authHeaderName")?.trim()?.takeIf { it.isNotEmpty() }
                val authHeaderValue = request.getParameter("authHeaderValue")?.trim()?.takeIf { it.isNotEmpty() }

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
                        WebhookPlatform.TEAMS -> "https://{tenant}.webhook.office.com/..., https://outlook.office.com/webhook/..., or https://{id}.environment.api.powerplatform.com/..."
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
                    try {
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
                            showBuildLink = showBuildLink,
                            showArtifacts = showArtifacts,
                            branchFilter = branchFilter,
                            authHeaderName = authHeaderName,
                            authHeaderValue = authHeaderValue
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
                    } catch (e: Exception) {
                        errors += "Failed to save webhook: ${e.message ?: "Unknown error"}"
                        mv.model["validationErrors"] = errors
                        mv.model["formUrl"] = webhookUrl ?: ""
                        mv.model["formPlatform"] = platformRaw ?: "SLACK"
                        mv.model["formOnStart"] = onStart
                        mv.model["formOnSuccess"] = onSuccess
                        mv.model["formOnFailure"] = onFailure
                        mv.model["formOnStall"] = onStall
                        mv.model["formBuildLongerThanAverage"] = buildLongerThanAverage
                        if (buildLongerThan != null) mv.model["formBuildLongerThan"] = buildLongerThan
                    }
                }
            } else if (action == "delete") {
                try {
                    val webhookIndexStr = request.getParameter("webhookIndex")
                    val webhookIndex = webhookIndexStr?.toIntOrNull()
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()

                    if (webhookIndex != null && webhookIndex >= 0 && webhookIndex < existingWebhooks.size) {
                        existingWebhooks.removeAt(webhookIndex)
                    }
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
                } catch (e: Exception) {
                    val errorMsg = "Failed to delete webhook: ${e.message ?: "Unknown error"}"
                    mv.model["validationErrors"] = listOf(errorMsg)
                }
            }
        }

        if (project != null) {
            if (buildTypeId != null) {
                val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                if (buildType != null) {
                    mv.model["webhooksWithSource"] = webhookManager.getWebhooksWithSourceForBuildType(buildType)
                    mv.model["buildTypeId"] = buildTypeId
                }
            } else {
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

    private fun resolveProject(projectId: String?, buildTypeId: String?): SProject? = when {
        !buildTypeId.isNullOrBlank() ->
            (sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId))?.project
        !projectId.isNullOrBlank() ->
            sBuildServer.projectManager.findProjectByExternalId(projectId)
                ?: sBuildServer.projectManager.findProjectById(projectId)
        else -> null
    }

    private fun canEditProject(request: HttpServletRequest, project: SProject?): Boolean {
        if (project == null) return false
        val user = SessionUser.getUser(request) ?: return false
        return user.isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)
    }

    private fun canViewProject(request: HttpServletRequest, project: SProject?): Boolean {
        if (project == null) return false
        val user = SessionUser.getUser(request) ?: return false
        return user.isPermissionGrantedForProject(project.projectId, Permission.VIEW_PROJECT)
    }

    private fun jsonEscape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun isValidWebhookUrl(platform: WebhookPlatform, url: String): Boolean {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isEmpty() || normalizedUrl.length > 2048) {
            return false
        }
        // A valid URL never contains raw whitespace (spaces must be percent-encoded)
        if (normalizedUrl.any { it.isWhitespace() }) {
            return false
        }

        return when (platform) {
            WebhookPlatform.SLACK -> {
                Regex("^https://hooks\\.slack\\.com/(services|workflows)/[A-Z0-9]+/[A-Z0-9]+/[A-Za-z0-9_-]+.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
            WebhookPlatform.TEAMS -> {
                Regex("^https://[a-zA-Z0-9_-]+\\.webhook\\.office\\.com/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                Regex("^https://outlook\\.office\\.com/webhook/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                Regex("^https://[a-zA-Z0-9_-]+\\.logic\\.azure\\.com/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                // New Power Automate host is {org}.{region}.environment.api.powerplatform.com (multiple labels before .environment)
                Regex("^https://[a-zA-Z0-9_.-]+\\.environment\\.api\\.powerplatform\\.com.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
            WebhookPlatform.DISCORD -> {
                Regex("^https://discord(?:app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
        }
    }
    
    private fun handleApiRequest(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        response.contentType = "application/json; charset=utf-8"
        
        val projectId = request.getParameter("projectId")
        val buildTypeId = request.getParameter("buildTypeId")

        val validEntity = when {
            !buildTypeId.isNullOrBlank() -> {
                val bt = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                    ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)
                bt != null
            }
            !projectId.isNullOrBlank() -> {
                val proj = sBuildServer.projectManager.findProjectByExternalId(projectId)
                    ?: sBuildServer.projectManager.findProjectById(projectId)
                proj != null
            }
            else -> false
        }
        
        if (!validEntity) {
            response.status = 400
            val errorMsg = when {
                !buildTypeId.isNullOrBlank() -> "Build Configuration not found: ${jsonEscape(buildTypeId)}"
                !projectId.isNullOrBlank() -> "Project not found: ${jsonEscape(projectId)}"
                else -> "Project or Build Configuration not found"
            }
            response.writer.write("""{"success":false,"error":"$errorMsg"}""")
            return null
        }

        // Reading requires view permission; mutating actions require edit permission.
        val targetProject = resolveProject(projectId, buildTypeId)
        if (request.method == "POST") {
            if (!canEditProject(request, targetProject)) {
                response.status = 403
                response.writer.write("""{"success":false,"error":"Forbidden: you do not have permission to edit this project"}""")
                return null
            }
        } else if (!canViewProject(request, targetProject)) {
            response.status = 403
            response.writer.write("""{"success":false,"error":"Forbidden: you do not have permission to view this project"}""")
            return null
        }

        when (request.method) {
            "GET" -> {
                val webhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId)
                val webhooksJson = webhooks.mapIndexed { index, webhook ->
                    """{
                        "index": $index,
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
                        "showBuildLink": ${webhook.showBuildLink},
                        "showArtifacts": ${webhook.showArtifacts},
                        "branchFilter": ${if (webhook.branchFilter != null) "\"${webhook.branchFilter.replace("\"", "\\\"")}\"" else "null"},
                        "hasAuth": ${!webhook.authHeaderName.isNullOrBlank() && !webhook.authHeaderValue.isNullOrBlank()},
                        "enabled": ${webhook.enabled}
                    }"""
                }.joinToString(",")
                response.writer.write("""{"success":true,"webhooks":[${webhooksJson}]}""")
            }
            "POST" -> {
                val action = request.getParameter("action")
                if (action == "delete") {
                    val webhookIndexStr = request.getParameter("webhookIndex")
                    val webhookIndex = webhookIndexStr?.toIntOrNull()

                    if (!buildTypeId.isNullOrBlank()) {
                        val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                            ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)

                        if (buildType == null) {
                            response.status = 404
                            response.writer.write("""{"success":false,"error":"Build configuration not found"}""")
                            return null
                        }

                        val webhooksWithSource = webhookManager.getWebhooksWithSourceForBuildType(buildType)

                        if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= webhooksWithSource.size) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
                            return null
                        }

                        val targetWebhook = webhooksWithSource[webhookIndex]

                        if (targetWebhook.source != WebhookSource.BUILD_TYPE) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Cannot delete inherited webhook. Go to the parent project to delete it."}""")
                            return null
                        }

                        try {
                            val localWebhooks = webhookManager.getWebhooksForEntity(null, buildTypeId).toMutableList()
                            val webhookUrl = targetWebhook.webhook.url
                            val removed = localWebhooks.removeIf { it.url == webhookUrl }

                            if (removed) {
                                webhookManager.saveWebhooksForEntity(null, buildTypeId, localWebhooks)
                                response.writer.write("""{"success":true,"message":"Webhook deleted successfully"}""")
                            } else {
                                response.status = 404
                                response.writer.write("""{"success":false,"error":"Webhook not found in local configuration"}""")
                            }
                        } catch (e: Exception) {
                            response.status = 500
                            val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                            response.writer.write("""{"success":false,"error":"Failed to delete webhook: $errorMsg"}""")
                        }
                    } else {
                        val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, null).toMutableList()

                        if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= existingWebhooks.size) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
                            return null
                        }

                        try {
                            existingWebhooks.removeAt(webhookIndex)
                            webhookManager.saveWebhooksForEntity(projectId, null, existingWebhooks)
                            response.writer.write("""{"success":true,"message":"Webhook deleted successfully"}""")
                        } catch (e: Exception) {
                            response.status = 500
                            val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                            response.writer.write("""{"success":false,"error":"Failed to delete webhook: $errorMsg"}""")
                        }
                    }
                    return null
                }
                if (action == "toggle") {
                    val webhookIndexStr = request.getParameter("webhookIndex")
                    val webhookIndex = webhookIndexStr?.toIntOrNull()

                    if (!buildTypeId.isNullOrBlank()) {
                        val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                            ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)

                        if (buildType == null) {
                            response.status = 404
                            response.writer.write("""{"success":false,"error":"Build configuration not found"}""")
                            return null
                        }

                        val webhooksWithSource = webhookManager.getWebhooksWithSourceForBuildType(buildType)

                        if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= webhooksWithSource.size) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
                            return null
                        }

                        val targetWebhook = webhooksWithSource[webhookIndex]

                        if (targetWebhook.source != WebhookSource.BUILD_TYPE) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Use toggleLocal action for inherited webhooks"}""")
                            return null
                        }

                        try {
                            val localWebhooks = webhookManager.getWebhooksForEntity(null, buildTypeId).toMutableList()
                            val webhookUrl = targetWebhook.webhook.url
                            val localIndex = localWebhooks.indexOfFirst { it.url == webhookUrl }

                            if (localIndex != -1) {
                                val oldWebhook = localWebhooks[localIndex]
                                val newWebhook = oldWebhook.copy(enabled = !oldWebhook.enabled)
                                localWebhooks[localIndex] = newWebhook
                                webhookManager.saveWebhooksForEntity(null, buildTypeId, localWebhooks)
                                response.writer.write("""{"success":true,"enabled":${newWebhook.enabled},"message":"Webhook ${if (newWebhook.enabled) "enabled" else "disabled"} successfully"}""")
                            } else {
                                response.status = 404
                                response.writer.write("""{"success":false,"error":"Webhook not found in local configuration"}""")
                            }
                        } catch (e: Exception) {
                            response.status = 500
                            val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                            response.writer.write("""{"success":false,"error":"Failed to toggle webhook: $errorMsg"}""")
                        }
                    } else {
                        val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, null).toMutableList()

                        if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= existingWebhooks.size) {
                            response.status = 400
                            response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
                            return null
                        }

                        try {
                            val oldWebhook = existingWebhooks[webhookIndex]
                            val newWebhook = oldWebhook.copy(enabled = !oldWebhook.enabled)
                            existingWebhooks[webhookIndex] = newWebhook
                            webhookManager.saveWebhooksForEntity(projectId, null, existingWebhooks)
                            response.writer.write("""{"success":true,"enabled":${newWebhook.enabled},"message":"Webhook ${if (newWebhook.enabled) "enabled" else "disabled"} successfully"}""")
                        } catch (e: Exception) {
                            response.status = 500
                            val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                            response.writer.write("""{"success":false,"error":"Failed to toggle webhook: $errorMsg"}""")
                        }
                    }
                    return null
                }
                if (action == "toggleLocal" && buildTypeId != null) {
                    val webhookIndexStr = request.getParameter("webhookIndex")
                    val webhookIndex = webhookIndexStr?.toIntOrNull()

                    val buildType = sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)
                        ?: sBuildServer.projectManager.findBuildTypeById(buildTypeId)

                    if (buildType == null) {
                        response.status = 404
                        response.writer.write("""{"success":false,"error":"Build configuration not found"}""")
                        return null
                    }

                    val webhooksWithSource = webhookManager.getWebhooksWithSourceForBuildType(buildType)

                    if (webhookIndex == null || webhookIndex < 0 || webhookIndex >= webhooksWithSource.size) {
                        response.status = 400
                        response.writer.write("""{"success":false,"error":"Invalid webhook index"}""")
                        return null
                    }

                    try {
                        val webhookUrl = webhooksWithSource[webhookIndex].webhook.url
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
                    } catch (e: Exception) {
                        response.status = 500
                        val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                        response.writer.write("""{"success":false,"error":"Failed to toggle local webhook: $errorMsg"}""")
                    }
                    return null
                }

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
                val showBuildLink = request.getParameter("showBuildLink")?.toBoolean() ?: true
                val showArtifacts = request.getParameter("showArtifacts")?.toBoolean() ?: true
                val branchFilter = request.getParameter("branchFilter")?.trim()?.takeIf { it.isNotEmpty() }
                val authHeaderName = request.getParameter("authHeaderName")?.trim()?.takeIf { it.isNotEmpty() }
                val authHeaderValue = request.getParameter("authHeaderValue")?.trim()?.takeIf { it.isNotEmpty() }

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
                    showBuildLink = showBuildLink,
                    showArtifacts = showArtifacts,
                    branchFilter = branchFilter,
                    authHeaderName = authHeaderName,
                    authHeaderValue = authHeaderValue
                )

                try {
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId).toMutableList()
                    existingWebhooks.add(newWebhook)
                    webhookManager.saveWebhooksForEntity(projectId, buildTypeId, existingWebhooks)

                    response.writer.write("""{"success":true,"message":"Webhook added successfully"}""")
                } catch (e: Exception) {
                    response.status = 500
                    val errorMsg = e.message?.replace("\"", "\\\"") ?: "Unknown error"
                    response.writer.write("""{"success":false,"error":"Failed to save webhook: $errorMsg"}""")
                }
            }
            else -> {
                response.status = 405
                response.writer.write("""{"success":false,"error":"Method not allowed"}""")
            }
        }
        
        return null
    }
}