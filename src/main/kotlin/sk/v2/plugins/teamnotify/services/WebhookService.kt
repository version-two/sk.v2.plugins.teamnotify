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
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import org.springframework.beans.factory.DisposableBean
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class WebhookService(
    private val sBuildServer: SBuildServer
) : DisposableBean {

    private val LOG = Logger.getInstance(WebhookService::class.java.name)
    private val slackPayloadGenerator = SlackPayloadGenerator()
    private val teamsPayloadGenerator = TeamsPayloadGenerator()
    private val discordPayloadGenerator = DiscordPayloadGenerator()

    // Notifications are delivered off the caller's thread. The build-event listener calls this on
    // TeamCity's event dispatch thread, and a blocking HTTP POST (up to 25s on timeout) per webhook
    // must never stall build processing. The queue is bounded; when it saturates (e.g. a flood of
    // builds while a webhook endpoint hangs) we DROP the rejected notification rather than run it on
    // the caller's thread, because stalling the build-event thread is never acceptable for a
    // best-effort notification. Each drop is logged so the saturation is visible to operators.
    private val executor: ExecutorService = ThreadPoolExecutor(
        4, 4, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(1000),
        ThreadFactory { r -> Thread(r, "teamnotify-webhook").apply { isDaemon = true } },
        RejectedExecutionHandler { _, exec ->
            if (!exec.isShutdown) {
                LOG.warn("Webhook delivery queue is saturated; dropping a notification. " +
                    "A webhook endpoint is likely slow or unreachable.")
            }
        }
    )

    override fun destroy() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

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
        // Build the context on the caller's (build-event) thread so the build state is read
        // consistently, then deliver only the HTTP request on a background thread so the event
        // thread is never blocked by webhook I/O.
        val ctx = try {
            buildContext(build, message, includeChanges, showBuildLink, showArtifacts)
        } catch (e: Exception) {
            LOG.warn("Failed to build webhook notification context for ${redact(url)}: ${e.message}")
            return
        }
        executor.submit {
            try {
                sendNotification(url, platform, ctx, authHeaderName, authHeaderValue)
            } catch (e: Exception) {
                LOG.warn("Failed to send webhook notification to ${redact(url)}: ${e.message}")
            }
        }
    }

    private fun buildContext(
        build: SRunningBuild,
        message: String,
        includeChanges: Boolean,
        showBuildLink: Boolean,
        showArtifacts: Boolean
    ): NotificationContext {
        return NotificationContext(
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
            // Only collect artifacts for finished builds that actually have artifacts
            if (!build.isFinished || !build.isArtifactsExists) return emptyList()

            val rootUrl = sBuildServer.rootUrl?.trimEnd('/') ?: return emptyList()
            val externalId = build.buildType?.externalId ?: return emptyList()

            // Enumerate the real artifact files via the TeamCity API rather than guessing paths.
            val result = mutableListOf<ArtifactSummary>()
            build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT)
                .iterateArtifacts(object : BuildArtifacts.BuildArtifactsProcessor {
                    override fun processBuildArtifact(artifact: BuildArtifact): BuildArtifacts.BuildArtifactsProcessor.Continuation {
                        if (artifact.isFile) {
                            val rel = artifact.relativePath
                            result.add(
                                ArtifactSummary(
                                    name = artifact.name,
                                    path = rel,
                                    size = artifact.size,
                                    downloadUrl = "$rootUrl/repository/download/$externalId/${build.buildId}:id/$rel"
                                )
                            )
                        }
                        return if (result.size >= MAX_ARTIFACTS) {
                            BuildArtifacts.BuildArtifactsProcessor.Continuation.BREAK
                        } else {
                            BuildArtifacts.BuildArtifactsProcessor.Continuation.CONTINUE
                        }
                    }
                })
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val MAX_ARTIFACTS = 10
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
            // Use the same "attachments" envelope the real Slack notifications use, so a successful
            // test confirms the endpoint accepts the actual delivery format (not just a bare {"text"}).
            WebhookPlatform.SLACK -> """
                {
                  "attachments": [{
                    "color": "#2eb886",
                    "title": "✅ TeamNotify test message",
                    "text": "This is a test message from TeamCity TeamNotify.",
                    "mrkdwn_in": ["text"]
                  }]
                }
            """.trimIndent()
            // Teams must use the same Adaptive Card "message"/"attachments" envelope the real
            // notifications use. The legacy {"text": ...} MessageCard shape is being retired with
            // Office 365 Connectors (final cutoff 2026-05-22) and is rejected by Power Automate
            // Workflow webhooks, so testing with it would not match real delivery.
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