package sk.v2.plugins.teamnotify.services

import com.intellij.openapi.diagnostic.Logger
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.payloads.DiscordPayloadGenerator
import sk.v2.plugins.teamnotify.payloads.SlackPayloadGenerator
import sk.v2.plugins.teamnotify.payloads.TeamsPayloadGenerator
import sk.v2.plugins.teamnotify.payloads.NotificationContext
import sk.v2.plugins.teamnotify.payloads.ChangeSummary
import sk.v2.plugins.teamnotify.payloads.ArtifactSummary
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.vcs.SVcsModification
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class WebhookService(
    private val sBuildServer: SBuildServer
) {

    private val LOG = Logger.getInstance(WebhookService::class.java.name)
    private val slackPayloadGenerator = SlackPayloadGenerator()
    private val teamsPayloadGenerator = TeamsPayloadGenerator()
    private val discordPayloadGenerator = DiscordPayloadGenerator()

    fun sendNotification(
        url: String,
        platform: WebhookPlatform,
        build: SRunningBuild,
        message: String,
        includeChanges: Boolean = true,
        authHeaderName: String? = null,
        authHeaderValue: String? = null,
        showBuildLink: Boolean = true,
        showArtifacts: Boolean = true
    ) {
        // Backward-compatible entrypoint: construct a minimal NotificationContext
        val ctx = NotificationContext(
            status = when {
                message.contains("started", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.STARTED
                message.contains("successful", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.SUCCESS
                message.contains("failed", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.FAILURE
                message.contains("cancelled", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.CANCELLED
                message.contains("stalled", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.STALLED
                message.contains("fixed", ignoreCase = true) -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.FIXED
                else -> sk.v2.plugins.teamnotify.payloads.NotificationStatus.STARTED
            },
            build = build,
            title = "",
            message = message,
            rootUrl = safeRootUrl(),
            buildUrl = safeBuildUrl(build),
            artifactsUrl = safeArtifactsUrl(build),
            projectName = build.buildType?.project?.name,
            buildTypeName = build.buildType?.name,
            buildExternalId = build.buildType?.externalId,
            buildNumber = build.buildNumber,
            triggeredBy = build.triggeredBy?.user?.descriptiveName,
            agentName = build.agentName,
            startTime = build.startDate,
            finishTime = build.finishDate,
            changes = if (includeChanges) collectRecentChanges(build, 5) else emptyList(),
            artifacts = collectArtifacts(build),
            showBuildLink = showBuildLink,
            showArtifacts = showArtifacts
        )
        sendNotification(url, platform, ctx, authHeaderName, authHeaderValue)
    }

    fun sendNotification(url: String, platform: WebhookPlatform, ctx: NotificationContext, authHeaderName: String? = null, authHeaderValue: String? = null) {
        val payload = when (platform) {
            WebhookPlatform.SLACK -> slackPayloadGenerator.generatePayload(ctx)
            WebhookPlatform.TEAMS -> teamsPayloadGenerator.generatePayload(ctx)
            WebhookPlatform.DISCORD -> discordPayloadGenerator.generatePayload(ctx)
        }
        val redactedUrl = redact(url)
        LOG.info("Dispatching webhook to $redactedUrl (Platform: $platform)")
        val result = postJson(url, payload, authHeaderName, authHeaderValue)
        if (result.success) {
            LOG.info("Webhook delivered to $redactedUrl (HTTP ${result.statusCode})")
        } else {
            LOG.warn("Webhook delivery FAILED to $redactedUrl (HTTP ${result.statusCode}). Error: ${result.errorBody ?: "<none>"}")
        }
    }

    private fun safeBuildUrl(build: SRunningBuild): String? {
        return try {
            val root = sBuildServer.rootUrl?.trimEnd('/') ?: return null
            "$root/viewLog.html?buildId=${build.buildId}"
        } catch (_: Exception) { null }
    }

    private fun safeRootUrl(): String? = try { sBuildServer.rootUrl } catch (_: Exception) { null }

    private fun safeArtifactsUrl(build: SRunningBuild): String? {
        return try {
            val root = sBuildServer.rootUrl?.trimEnd('/') ?: return null
            "$root/viewLog.html?buildId=${build.buildId}&tab=artifacts"
        } catch (_: Exception) { null }
    }

    private fun collectRecentChanges(build: SRunningBuild, limit: Int): List<ChangeSummary> {
        return try {
            val mods = try {
                val list = build.containingChanges
                if (list != null) list.toList() else emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
            mods.take(limit).map { m ->
                val version = try { m.version } catch (_: Throwable) { null }
                val user = try { m.userName } catch (_: Throwable) { null }
                val comment = try { m.description } catch (_: Throwable) { null }
                ChangeSummary(version = version, user = user, comment = comment)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    private fun collectArtifacts(build: SRunningBuild): List<ArtifactSummary> {
        return try {
            // Only collect artifacts for finished builds
            if (!build.isFinished) return emptyList()
            
            val rootUrl = sBuildServer.rootUrl?.trimEnd('/') ?: return emptyList()
            val buildType = build.buildType ?: return emptyList()
            
            val artifacts = mutableListOf<ArtifactSummary>()
            
            // TeamCity's plugin API doesn't provide direct access to enumerate artifact files
            // The recommended approach is to use REST API or know the artifact paths beforehand
            
            // For known artifact patterns in common build types, we can provide direct links
            // These are based on common conventions:
            
            // 1. Check if build has artifacts at all
            if (build.isArtifactsExists) {
                // 2. For plugin builds (like this one), artifacts are typically in distributions/
                if (buildType.name.contains("plugin", ignoreCase = true) || 
                    buildType.project.name.contains("notify", ignoreCase = true)) {
                    // TeamCity plugins typically produce a ZIP in build/distributions/
                    val projectName = buildType.project.name.replace(Regex("[^a-zA-Z0-9-]"), "-").lowercase()
                    val buildNumber = build.buildNumber ?: "unknown"
                    
                    // Standard plugin distribution ZIP
                    artifacts.add(ArtifactSummary(
                        name = "team-notify-${buildNumber}.zip",
                        path = "distributions/team-notify-*.zip",
                        size = null,
                        downloadUrl = "$rootUrl/repository/download/${buildType.externalId}/${build.buildId}:id/distributions/team-notify-*.zip"
                    ))
                }
                
                // 3. For Java/Kotlin projects, common artifacts
                if (buildType.name.contains("java", ignoreCase = true) || 
                    buildType.name.contains("kotlin", ignoreCase = true) ||
                    buildType.name.contains("jar", ignoreCase = true)) {
                    // JAR files in build/libs/
                    artifacts.add(ArtifactSummary(
                        name = "Application JAR",
                        path = "build/libs/*.jar",
                        size = null,
                        downloadUrl = "$rootUrl/repository/download/${buildType.externalId}/${build.buildId}:id/build/libs/*.jar"
                    ))
                }
                
                // 4. For web projects
                if (buildType.name.contains("web", ignoreCase = true) || 
                    buildType.name.contains("dist", ignoreCase = true)) {
                    artifacts.add(ArtifactSummary(
                        name = "Distribution Package",
                        path = "dist.zip",
                        size = null,
                        downloadUrl = "$rootUrl/repository/download/${buildType.externalId}/${build.buildId}:id/dist.zip"
                    ))
                }
            }
            
            // Return empty list if no artifacts detected
            // The payload generators will fall back to "Browse Artifacts" link
            artifacts
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class HttpResult(
        val success: Boolean,
        val statusCode: Int,
        val responseBody: String? = null,
        val errorBody: String? = null
    )

    private fun postJson(urlString: String, jsonBody: String, authHeaderName: String? = null, authHeaderValue: String? = null): HttpResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json, */*")
                // Add auth header if provided (for Power Automate Workflows with authentication)
                if (!authHeaderName.isNullOrBlank() && !authHeaderValue.isNullOrBlank()) {
                    setRequestProperty(authHeaderName, authHeaderValue)
                }
            }

            val bytes = jsonBody.toByteArray(StandardCharsets.UTF_8)
            connection.outputStream.use { os: OutputStream ->
                os.write(bytes)
                os.flush()
            }

            val status = connection.responseCode
            val success = status in 200..299
            val body = tryReadBody(connection)
            val err = if (success) null else tryReadError(connection)
            HttpResult(success, status, body, err)
        } catch (e: Exception) {
            LOG.warn("HTTP POST failed: ${e.message}")
            HttpResult(false, -1, null, e.message)
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun tryReadBody(conn: HttpURLConnection): String? = try {
        BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
    } catch (_: Exception) { null }

    private fun tryReadError(conn: HttpURLConnection): String? = try {
        BufferedReader(InputStreamReader(conn.errorStream, StandardCharsets.UTF_8)).use { it.readText() }
    } catch (_: Exception) { null }

    private fun redact(url: String): String {
        return try {
            val uri = URL(url)
            val parts = uri.path.split('/').toMutableList()
            if (parts.size > 3) {
                for (i in 2 until parts.size) {
                    if (parts[i].isNotBlank()) parts[i] = "***"
                }
            }
            uri.protocol + "://" + uri.host + parts.joinToString(separator = "/", prefix = "")
        } catch (_: Exception) {
            if (url.length > 32) url.substring(0, 32) + "***" else "***"
        }
    }

    data class TestResult(val success: Boolean, val statusCode: Int, val errorBody: String?)

    fun testWebhook(url: String, platform: WebhookPlatform, authHeaderName: String? = null, authHeaderValue: String? = null): TestResult {
        val payload = when (platform) {
            WebhookPlatform.SLACK -> """{"text": "Test message from TeamNotify"}"""
            // Teams must use the same Adaptive Card "message"/"attachments" envelope the real
            // notifications use. The legacy {"text": ...} MessageCard shape is being retired with
            // Office 365 Connectors (2026-03-31) and is rejected by Power Automate Workflow webhooks,
            // so testing with it would not match real delivery.
            WebhookPlatform.TEAMS -> """
                {
                  "type": "message",
                  "attachments": [{
                    "contentType": "application/vnd.microsoft.card.adaptive",
                    "contentUrl": null,
                    "content": {
                      "${"$"}schema": "http://adaptivecards.io/schemas/adaptive-card.json",
                      "type": "AdaptiveCard",
                      "version": "1.2",
                      "body": [{
                        "type": "TextBlock",
                        "text": "✅ TeamNotify test message",
                        "weight": "Bolder",
                        "size": "Large",
                        "color": "Good",
                        "wrap": true
                      },{
                        "type": "TextBlock",
                        "text": "This is a test message from TeamCity TeamNotify.",
                        "wrap": true,
                        "spacing": "Small"
                      }]
                    }
                  }]
                }
            """.trimIndent()
            WebhookPlatform.DISCORD -> """
                {
                  "embeds": [
                    {
                      "title": "TeamNotify Test",
                      "description": "This is a test message from TeamCity TeamNotify",
                      "color": 7506394
                    }
                  ]
                }
            """.trimIndent()
        }
        val redactedUrl = redact(url)
        LOG.info("Testing webhook delivery to $redactedUrl (Platform: $platform)")
        val result = postJson(url, payload, authHeaderName, authHeaderValue)
        if (result.success) {
            LOG.info("Test webhook delivered to $redactedUrl (HTTP ${result.statusCode})")
        } else {
            LOG.warn("Test webhook delivery FAILED to $redactedUrl (HTTP ${result.statusCode}). Error: ${result.errorBody ?: "<none>"}")
        }
        return TestResult(result.success, result.statusCode, result.errorBody)
    }
}