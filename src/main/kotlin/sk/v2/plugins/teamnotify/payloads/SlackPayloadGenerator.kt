package sk.v2.plugins.teamnotify.payloads

class SlackPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val buildTypeName = ctx.buildTypeName.orEmpty()
        val buildNumber = ctx.buildNumber.orEmpty()
        val link = ctx.buildUrl ?: ""
        val linkText = if (link.isNotEmpty()) " (<${link}|Open in TeamCity>)" else ""
        val title = if (ctx.title.isNotBlank()) ctx.title else "TeamCity Build Notification"
        val text = "[${buildTypeName}] ${ctx.message} - Build ${buildNumber}${linkText}"

        // Build compact changes summary on a single line
        val changesSummary = if (ctx.changes.isNotEmpty()) {
            val items = ctx.changes.take(3).map { ch ->
                val who = (ch.user ?: "").ifBlank { "unknown" }
                val msg = (ch.comment ?: "").replace("\n", " ").trim()
                val shortMsg = if (msg.length > 60) msg.substring(0, 57) + "…" else msg
                val rev = (ch.version ?: "").take(10)
                "${who}: ${shortMsg}${if (rev.isNotEmpty()) " (${rev})" else ""}"
            }
            " | Changes: " + items.joinToString(", ")
        } else ""

        val combined = (title + ": " + text + changesSummary)
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        return """{"text": "${combined}"}"""
    }
}
