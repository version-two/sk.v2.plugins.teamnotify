package sk.v2.plugins.teamnotify.payloads

class TeamsPayloadGenerator : PayloadGenerator {
    override fun generatePayload(ctx: NotificationContext): String {
        val buildTypeName = ctx.buildTypeName.orEmpty()
        val buildNumber = ctx.buildNumber.orEmpty()
        val link = ctx.buildUrl ?: ""
        val linkText = if (link.isNotEmpty()) " (<${link}|Open in TeamCity>)" else ""
        val title = if (ctx.title.isNotBlank()) ctx.title else "TeamCity Build Notification"
        val text = "[$buildTypeName] ${ctx.message} - Build $buildNumber$linkText"
        return """{"text": "${title.replace("\"", "\\\"")}: ${text.replace("\"", "\\\"")}"}"""
    }
}
