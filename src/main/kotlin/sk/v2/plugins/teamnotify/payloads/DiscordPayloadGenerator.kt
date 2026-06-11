package sk.v2.plugins.teamnotify.payloads

class DiscordPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val project = (ctx.projectName ?: "Unknown Project").trim()
        val config = (ctx.buildTypeName ?: "Unknown Config").trim()
        val buildNo = (ctx.buildNumber ?: "?").trim()
        
        val titlePrefix = "$project - $config - Build #$buildNo"
        
        val title = when (ctx.status) {
            NotificationStatus.STARTED -> "▶️ $titlePrefix Started"
            NotificationStatus.SUCCESS -> "✅ $titlePrefix Successful"
            NotificationStatus.FAILURE -> "❌ $titlePrefix Failed"
            NotificationStatus.STALLED -> "⚠️ $titlePrefix Stalled"
            NotificationStatus.CANCELLED -> "🚫 $titlePrefix Cancelled"
            NotificationStatus.FIXED -> "🎉 $titlePrefix Fixed"
            NotificationStatus.FIRST_FAILURE -> "🚨 $titlePrefix - First Failure"
            NotificationStatus.LONGER_THAN -> "⏰ $titlePrefix - Long Duration"
            NotificationStatus.LONGER_THAN_AVERAGE -> "📈 $titlePrefix - Longer Than Average"
        }

        val color = when (ctx.status) {
            NotificationStatus.STARTED -> 3447003   // blue (#3498DB)
            NotificationStatus.SUCCESS -> 3066993   // green (#2ECC71)
            NotificationStatus.FAILURE -> 15158332  // red (#E74C3C)
            NotificationStatus.STALLED -> 16098851  // orange (#F5A623)
            NotificationStatus.CANCELLED -> 15158332 // red (#E74C3C) - same as failure
            NotificationStatus.FIXED -> 10181046    // purple (#9B59B6)
            NotificationStatus.FIRST_FAILURE -> 15158332 // red
            NotificationStatus.LONGER_THAN, NotificationStatus.LONGER_THAN_AVERAGE -> 15105570 // yellow (#E67E22)
        }
        val triggeredBy = (ctx.triggeredBy ?: "").trim()
        val buildUrl = (ctx.buildUrl ?: "").trim()
        val artifactsUrl = (ctx.artifactsUrl ?: "").trim()

        val fields = mutableListOf<String>()
        if (project.isNotEmpty()) fields += fieldJson("Project", project, true)
        if (config.isNotEmpty()) fields += fieldJson("Build Config", config, true)
        if (buildNo.isNotEmpty()) fields += fieldJson("Build #", buildNo, true)
        if (triggeredBy.isNotEmpty()) fields += fieldJson("Triggered by", triggeredBy, true)

        // Build link - controlled by ctx.showBuildLink.
        // Field values are JSON-escaped once inside fieldJson(); do NOT pre-escape here or the value
        // is double-escaped and renders literal \" / \\ sequences in Discord.
        if (ctx.showBuildLink && buildUrl.isNotEmpty()) {
            fields += fieldJson("Build", "[Open in TeamCity](${buildUrl})", false)
        }

        // Artifacts - controlled by ctx.showArtifacts
        // Only show artifacts for completed builds (success, fixed, or completed failures)
        val isCompletedBuild = ctx.status in listOf(
            NotificationStatus.SUCCESS,
            NotificationStatus.FIXED,
            NotificationStatus.FAILURE,
            NotificationStatus.FIRST_FAILURE
        )

        // Show individual artifact links if available, otherwise show browse link
        if (ctx.showArtifacts && isCompletedBuild) {
            if (ctx.artifacts.isNotEmpty()) {
                // Show individual artifact download links
                val artifactLinks = ctx.artifacts.take(5).map { artifact ->
                    "[${artifact.name}](${artifact.downloadUrl})"
                }.joinToString(" • ")
                fields += fieldJson("Artifacts", artifactLinks, false)
            } else if (artifactsUrl.isNotEmpty()) {
                // Fallback to artifact browser link
                fields += fieldJson("Artifacts", "[Browse All Artifacts](${artifactsUrl})", false)
            }
        }

        // Show changes for all build notifications (not just STARTED)
        if (ctx.changes.isNotEmpty()) {
            val items = ctx.changes.take(3).map { ch ->
                val who = (ch.user ?: "").ifBlank { "unknown" }
                val msg = (ch.comment ?: "").replace("\n", " ").trim()
                val shortMsg = if (msg.length > 80) msg.substring(0, 77) + "…" else msg
                val rev = (ch.version ?: "").take(10)
                val suffix = if (rev.isNotEmpty()) " (${rev})" else ""
                "• ${who}: ${shortMsg}${suffix}"
            }
            val value = items.joinToString("\n")
            fields += fieldJson("Changes", value, false)
        }

        val description = escape(ctx.message)

        val embed = buildString {
            append("{")
            append("\"title\":\"").append(escape(title)).append("\",")
            append("\"description\":\"").append(description).append("\",")
            append("\"color\":").append(color).append(",")
            if (fields.isNotEmpty()) {
                append("\"fields\":[").append(fields.joinToString(","))
                append("]")
            } else {
                // remove trailing comma if no fields will be appended
                if (endsWith(',')) deleteCharAt(length - 1)
            }
            append("}")
        }

        return "{" + "\"embeds\":[" + embed + "]}"
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun fieldJson(name: String, value: String, inline: Boolean): String {
        return "{" +
            "\"name\":\"" + escape(name) + "\"," +
            "\"value\":\"" + escape(value) + "\"," +
            "\"inline\":" + inline +
            "}"
    }
}
