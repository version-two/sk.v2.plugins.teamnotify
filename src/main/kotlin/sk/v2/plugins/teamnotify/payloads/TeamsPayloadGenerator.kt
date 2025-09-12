package sk.v2.plugins.teamnotify.payloads

class TeamsPayloadGenerator : PayloadGenerator {
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

        val themeColor = when (ctx.status) {
            NotificationStatus.STARTED -> "0088cc"      // blue
            NotificationStatus.SUCCESS -> "2eb886"      // green
            NotificationStatus.FAILURE -> "dc3545"      // red
            NotificationStatus.STALLED -> "f48924"      // orange
            NotificationStatus.CANCELLED -> "dc3545"    // red - same as failure
            NotificationStatus.FIXED -> "9b59b6"        // purple
            NotificationStatus.FIRST_FAILURE -> "dc3545"
            NotificationStatus.LONGER_THAN, NotificationStatus.LONGER_THAN_AVERAGE -> "e67e22" // yellow/orange
        }
        val triggeredBy = (ctx.triggeredBy ?: "").trim()
        val buildUrl = (ctx.buildUrl ?: "").trim()
        val artifactsUrl = (ctx.artifactsUrl ?: "").trim()

        // Build facts array for card
        val facts = mutableListOf<String>()
        if (project.isNotEmpty()) facts += factJson("Project", project)
        if (config.isNotEmpty()) facts += factJson("Build Config", config)
        if (buildNo.isNotEmpty()) facts += factJson("Build #", buildNo)
        if (triggeredBy.isNotEmpty()) facts += factJson("Triggered by", triggeredBy)

        // Build changes section - only for build started
        val changesSection = if (ctx.status == NotificationStatus.STARTED && ctx.changes.isNotEmpty()) {
            val items = ctx.changes.take(3).map { ch ->
                val who = (ch.user ?: "").ifBlank { "unknown" }
                val msg = (ch.comment ?: "").replace("\n", " ").trim()
                val shortMsg = if (msg.length > 80) msg.substring(0, 77) + "…" else msg
                val rev = (ch.version ?: "").take(10)
                val suffix = if (rev.isNotEmpty()) " (${rev})" else ""
                "• **${escape(who)}**: ${escape(shortMsg)}${suffix}"
            }
            val changesText = items.joinToString("\\n\\n")
            """,{
                "type": "TextBlock",
                "text": "**Recent Changes:**",
                "wrap": true,
                "separator": true,
                "spacing": "Medium"
            },{
                "type": "TextBlock",
                "text": "${escape(changesText)}",
                "wrap": true,
                "isSubtle": true,
                "spacing": "Small"
            }"""
        } else ""

        // Build actions array
        val actions = mutableListOf<String>()
        if (buildUrl.isNotEmpty()) {
            actions += """{
                "type": "Action.OpenUrl",
                "title": "View Build",
                "url": "${escape(buildUrl)}"
            }"""
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
                    actions += """{
                        "type": "Action.OpenUrl",
                        "title": "${escape(artifact.name)}",
                        "url": "${escape(artifact.downloadUrl)}"
                    }"""
                }
                // If there are more artifacts, add a browse all button
                if (ctx.artifacts.size > 3 && artifactsUrl.isNotEmpty()) {
                    actions += """{
                        "type": "Action.OpenUrl",
                        "title": "More artifacts...",
                        "url": "${escape(artifactsUrl)}"
                    }"""
                }
            } else if (artifactsUrl.isNotEmpty()) {
                // Fallback to artifact browser button
                actions += """{
                    "type": "Action.OpenUrl",
                    "title": "Browse Artifacts",
                    "url": "${escape(artifactsUrl)}"
                }"""
            }
        }

        // Build the Adaptive Card
        val card = """{
            "type": "message",
            "attachments": [{
                "contentType": "application/vnd.microsoft.card.adaptive",
                "contentUrl": null,
                "content": {
                    "${"$"}schema": "http://adaptivecards.io/schemas/adaptive-card.json",
                    "type": "AdaptiveCard",
                    "version": "1.2",
                    "body": [
                        {
                            "type": "TextBlock",
                            "text": "${escape(title)}",
                            "weight": "Bolder",
                            "size": "Large",
                            "wrap": true
                        },
                        {
                            "type": "TextBlock",
                            "text": "${escape(ctx.message)}",
                            "wrap": true,
                            "spacing": "Small"
                        }${if (facts.isNotEmpty()) """,{
                            "type": "FactSet",
                            "facts": [${facts.joinToString(",")}],
                            "separator": true,
                            "spacing": "Medium"
                        }""" else ""}${changesSection}
                    ]${if (actions.isNotEmpty()) """,
                    "actions": [${actions.joinToString(",")}]""" else ""}
                }
            }]
        }"""

        return card
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun factJson(name: String, value: String): String {
        return """{
            "title": "${escape(name)}",
            "value": "${escape(value)}"
        }"""
    }
}
