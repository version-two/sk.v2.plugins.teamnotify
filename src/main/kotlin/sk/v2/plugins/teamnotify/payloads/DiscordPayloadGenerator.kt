package sk.v2.plugins.teamnotify.payloads

class DiscordPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val title = when (ctx.status) {
            NotificationStatus.STARTED -> "Build Started"
            NotificationStatus.SUCCESS -> "Build Successful"
            NotificationStatus.FAILURE -> "Build Failed"
            NotificationStatus.STALLED -> "Build Stalled"
            NotificationStatus.FIXED -> "Build Fixed"
            NotificationStatus.FIRST_FAILURE -> "First Failure"
            NotificationStatus.LONGER_THAN -> "Long Build Duration"
            NotificationStatus.LONGER_THAN_AVERAGE -> "Longer Than Average"
        }

        val color = when (ctx.status) {
            NotificationStatus.STARTED -> 3447003   // blue
            NotificationStatus.SUCCESS -> 3066993   // green
            NotificationStatus.FAILURE -> 15158332  // red
            NotificationStatus.STALLED -> 16098851  // orange
            NotificationStatus.FIXED -> 10181046    // purple
            NotificationStatus.FIRST_FAILURE -> 15158332
            NotificationStatus.LONGER_THAN, NotificationStatus.LONGER_THAN_AVERAGE -> 15105570 // yellow
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
        if (buildUrl.isNotEmpty()) fields += fieldJson("Build", "[Open in TeamCity](${escape(buildUrl)})", false)
        if (artifactsUrl.isNotEmpty()) fields += fieldJson("Artifacts", "[Browse Artifacts](${escape(artifactsUrl)})", false)

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

    private fun fieldJson(name: String, value: String, inline: Boolean): String {
        return "{" +
            "\"name\":\"" + escape(name) + "\"," +
            "\"value\":\"" + escape(value) + "\"," +
            "\"inline\":" + inline +
            "}"
    }
}
