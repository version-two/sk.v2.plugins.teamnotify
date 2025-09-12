package sk.v2.plugins.teamnotify.payloads

class SlackPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val title = when (ctx.status) {
            NotificationStatus.STARTED -> ":arrow_forward: Build Started"
            NotificationStatus.SUCCESS -> ":white_check_mark: Build Successful"
            NotificationStatus.FAILURE -> ":x: Build Failed"
            NotificationStatus.STALLED -> ":warning: Build Stalled"
            NotificationStatus.FIXED -> ":tada: Build Fixed"
            NotificationStatus.FIRST_FAILURE -> ":rotating_light: First Failure"
            NotificationStatus.LONGER_THAN -> ":clock3: Long Build Duration"
            NotificationStatus.LONGER_THAN_AVERAGE -> ":chart_with_upwards_trend: Longer Than Average"
        }

        val color = when (ctx.status) {
            NotificationStatus.STARTED -> "#0088cc"     // blue
            NotificationStatus.SUCCESS -> "#2eb886"     // green
            NotificationStatus.FAILURE -> "#dc3545"     // red
            NotificationStatus.STALLED -> "#f48924"     // orange
            NotificationStatus.FIXED -> "#9b59b6"       // purple
            NotificationStatus.FIRST_FAILURE -> "#dc3545"
            NotificationStatus.LONGER_THAN, NotificationStatus.LONGER_THAN_AVERAGE -> "#e67e22" // yellow/orange
        }

        val project = (ctx.projectName ?: "").trim()
        val config = (ctx.buildTypeName ?: "").trim()
        val buildNo = (ctx.buildNumber ?: "").trim()
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
        if (artifactsUrl.isNotEmpty()) {
            actions += actionJson("Browse Artifacts", artifactsUrl, "default")
        }

        // Build changes text
        val changesText = if (ctx.changes.isNotEmpty()) {
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
