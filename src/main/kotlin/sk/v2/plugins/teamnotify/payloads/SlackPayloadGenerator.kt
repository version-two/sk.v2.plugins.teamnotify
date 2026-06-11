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

        // The status color is applied as the attachment's left-border color. The message body itself
        // is built with Block Kit blocks (the modern Slack format), because legacy attachment "actions"
        // buttons are silently dropped by incoming webhooks. Incoming webhooks DO render Block Kit
        // blocks (including URL buttons) nested inside an attachment, which keeps the colored bar.
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

        val blocks = mutableListOf<String>()

        // Title and message
        blocks += section("*${title}*")
        if (ctx.message.isNotBlank()) {
            blocks += section(ctx.message)
        }

        // Facts as a two-column field section
        val fields = mutableListOf<String>()
        if (project.isNotEmpty()) fields += mrkdwnField("Project", project)
        if (config.isNotEmpty()) fields += mrkdwnField("Build Config", config)
        if (buildNo.isNotEmpty()) fields += mrkdwnField("Build #", buildNo)
        if (triggeredBy.isNotEmpty()) fields += mrkdwnField("Triggered by", triggeredBy)
        if (fields.isNotEmpty()) {
            blocks += """{"type":"section","fields":[${fields.joinToString(",")}]}"""
        }

        // Recent changes - show for all notifications
        if (ctx.changes.isNotEmpty()) {
            val items = ctx.changes.take(3).map { ch ->
                val who = (ch.user ?: "").ifBlank { "unknown" }
                val msg = (ch.comment ?: "").replace("\n", " ").trim()
                val shortMsg = if (msg.length > 80) msg.substring(0, 77) + "…" else msg
                val rev = (ch.version ?: "").take(10)
                val suffix = if (rev.isNotEmpty()) " `${rev}`" else ""
                "• *${who}*: ${shortMsg}${suffix}"
            }
            // Built raw; section() applies the single JSON escape.
            blocks += section("*Recent Changes:*\n" + items.joinToString("\n"))
        }

        // Action buttons (real Block Kit URL buttons - these render via incoming webhooks)
        val actions = mutableListOf<String>()

        // Build link - controlled by ctx.showBuildLink
        if (ctx.showBuildLink && buildUrl.isNotEmpty()) {
            actions += urlButton("View Build", buildUrl, primary = true)
        }

        // Only show artifacts for completed builds
        val isCompletedBuild = ctx.status in listOf(
            NotificationStatus.SUCCESS,
            NotificationStatus.FIXED,
            NotificationStatus.FAILURE,
            NotificationStatus.FIRST_FAILURE
        )

        // Show individual artifact buttons if available - controlled by ctx.showArtifacts
        if (ctx.showArtifacts && isCompletedBuild) {
            if (ctx.artifacts.isNotEmpty()) {
                // Add individual artifact download buttons (limit to 3 for space)
                ctx.artifacts.take(3).forEach { artifact ->
                    actions += urlButton(artifact.name, artifact.downloadUrl)
                }
                // If there are more artifacts, add a browse all button
                if (ctx.artifacts.size > 3 && artifactsUrl.isNotEmpty()) {
                    actions += urlButton("More artifacts...", artifactsUrl)
                }
            } else if (artifactsUrl.isNotEmpty()) {
                // Fallback to artifact browser button
                actions += urlButton("Browse Artifacts", artifactsUrl)
            }
        }

        if (actions.isNotEmpty()) {
            blocks += """{"type":"actions","elements":[${actions.joinToString(",")}]}"""
        }

        val attachment = """{"color":"${color}","blocks":[${blocks.joinToString(",")}]}"""
        return """{"attachments":[${attachment}]}"""
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    // A Block Kit section with a single mrkdwn text object. Receives RAW text and escapes once.
    private fun section(rawMrkdwn: String): String =
        """{"type":"section","text":{"type":"mrkdwn","text":"${escape(rawMrkdwn)}"}}"""

    // A single mrkdwn field for a two-column section: bold title above the value.
    private fun mrkdwnField(title: String, value: String): String =
        """{"type":"mrkdwn","text":"*${escape(title)}:*\n${escape(value)}"}"""

    // A Block Kit URL button. The label is plain_text (Slack caps it at 75 chars). A URL button opens
    // the link directly and needs no interactivity handler, so it works from a plain incoming webhook.
    private fun urlButton(text: String, url: String, primary: Boolean = false): String {
        val label = if (text.length > 75) text.substring(0, 74) + "…" else text
        val styleField = if (primary) ""","style":"primary"""" else ""
        return """{"type":"button","text":{"type":"plain_text","text":"${escape(label)}","emoji":true},"url":"${escape(url)}"${styleField}}"""
    }
}
