package sk.v2.plugins.teamnotify.payloads

class SlackPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val project = (ctx.projectName ?: "Unknown Project").trim()
        val config = (ctx.buildTypeName ?: "Unknown Config").trim()
        val buildNo = (ctx.buildNumber ?: "?").trim()
        
        val titlePrefix = "$project - $config - Build #$buildNo"
        
        val title = when (ctx.status) {
            NotificationStatus.STARTED -> ":arrow_forward: $titlePrefix Started"
            NotificationStatus.SUCCESS -> ":white_check_mark: $titlePrefix Successful"
            NotificationStatus.FAILURE -> ":x: $titlePrefix Failed"
            NotificationStatus.STALLED -> ":warning: $titlePrefix Stalled"
            NotificationStatus.CANCELLED -> ":no_entry_sign: $titlePrefix Cancelled"
            NotificationStatus.FIXED -> ":tada: $titlePrefix Fixed"
            NotificationStatus.FIRST_FAILURE -> ":rotating_light: $titlePrefix - First Failure"
            NotificationStatus.LONGER_THAN -> ":clock3: $titlePrefix - Long Duration"
            NotificationStatus.LONGER_THAN_AVERAGE -> ":chart_with_upwards_trend: $titlePrefix - Longer Than Average"
        }

        val color = when (ctx.status) {
            NotificationStatus.STARTED -> "#0088cc"     // blue
            NotificationStatus.SUCCESS -> "#2eb886"     // green
            NotificationStatus.FAILURE -> "#dc3545"     // red
            NotificationStatus.STALLED -> "#f48924"     // orange
            NotificationStatus.CANCELLED -> "#dc3545"   // red - same as failure
            NotificationStatus.FIXED -> "#9b59b6"       // purple
            NotificationStatus.FIRST_FAILURE -> "#dc3545"
            NotificationStatus.LONGER_THAN, NotificationStatus.LONGER_THAN_AVERAGE -> "#e67e22" // yellow/orange
        }
        val triggeredBy = (ctx.triggeredBy ?: "").trim()
        val buildUrl = (ctx.buildUrl ?: "").trim()
        val artifactsUrl = (ctx.artifactsUrl ?: "").trim()

        val fields = mutableListOf<String>()
        if (project.isNotEmpty()) fields += fieldJson("Project", project, true)
        if (config.isNotEmpty()) fields += fieldJson("Build Config", config, true)
        if (buildNo.isNotEmpty()) fields += fieldJson("Build #", buildNo, true)
        if (triggeredBy.isNotEmpty()) fields += fieldJson("Triggered by", triggeredBy, true)

        // Actions as buttons in Slack
        val actions = mutableListOf<String>()
        if (buildUrl.isNotEmpty()) {
            actions += actionJson("View Build", buildUrl, "primary")
        }
        
        // Only show artifacts for completed builds
        val showArtifacts = ctx.status in listOf(
            NotificationStatus.SUCCESS,
            NotificationStatus.FIXED,
            NotificationStatus.FAILURE,
            NotificationStatus.FIRST_FAILURE
        )
        
        // Show individual artifact buttons if available
        if (showArtifacts) {
            if (ctx.artifacts.isNotEmpty()) {
                // Add individual artifact download buttons (limit to 3 for space)
                ctx.artifacts.take(3).forEach { artifact ->
                    actions += actionJson(artifact.name, artifact.downloadUrl, "default")
                }
                // If there are more artifacts, add a browse all button
                if (ctx.artifacts.size > 3 && artifactsUrl.isNotEmpty()) {
                    actions += actionJson("More artifacts...", artifactsUrl, "default")
                }
            } else if (artifactsUrl.isNotEmpty()) {
                // Fallback to artifact browser button
                actions += actionJson("Browse Artifacts", artifactsUrl, "default")
            }
        }

        // Build changes text - only for build started
        val changesText = if (ctx.status == NotificationStatus.STARTED && ctx.changes.isNotEmpty()) {
            val items = ctx.changes.take(3).map { ch ->
                val who = (ch.user ?: "").ifBlank { "unknown" }
                val msg = (ch.comment ?: "").replace("\n", " ").trim()
                val shortMsg = if (msg.length > 80) msg.substring(0, 77) + "…" else msg
                val rev = (ch.version ?: "").take(10)
                val suffix = if (rev.isNotEmpty()) " `${rev}`" else ""
                "• *${escape(who)}*: ${escape(shortMsg)}${suffix}"
            }
            "*Recent Changes:*\n" + items.joinToString("\n")
        } else ""

        val attachment = buildString {
            append("{")
            append("\"color\":\"").append(color).append("\",")
            append("\"title\":\"").append(escape(title)).append("\",")
            append("\"text\":\"").append(escape(ctx.message)).append("\",")
            if (fields.isNotEmpty()) {
                append("\"fields\":[").append(fields.joinToString(",")).append("],")
            }
            if (changesText.isNotEmpty()) {
                append("\"footer\":\"").append(escape(changesText)).append("\",")
            }
            if (actions.isNotEmpty()) {
                append("\"actions\":[").append(actions.joinToString(",")).append("],")
            }
            append("\"mrkdwn_in\":[\"text\",\"pretext\",\"footer\"]")
            append("}")
        }

        return "{\"attachments\":[${attachment}]}"
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    private fun fieldJson(title: String, value: String, short: Boolean): String {
        return "{" +
            "\"title\":\"" + escape(title) + "\"," +
            "\"value\":\"" + escape(value) + "\"," +
            "\"short\":" + short +
            "}"
    }

    private fun actionJson(text: String, url: String, style: String): String {
        return "{" +
            "\"type\":\"button\"," +
            "\"text\":\"" + escape(text) + "\"," +
            "\"url\":\"" + escape(url) + "\"," +
            "\"style\":\"" + style + "\"" +
            "}"
    }
}
