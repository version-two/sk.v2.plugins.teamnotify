package sk.v2.plugins.teamnotify.services

import com.intellij.openapi.diagnostic.Logger
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.payloads.DiscordPayloadGenerator
import sk.v2.plugins.teamnotify.payloads.SlackPayloadGenerator
import sk.v2.plugins.teamnotify.payloads.TeamsPayloadGenerator
import jetbrains.buildServer.serverSide.SRunningBuild

class WebhookService {

    private val LOG = Logger.getInstance(WebhookService::class.java.name)
    private val slackPayloadGenerator = SlackPayloadGenerator()
    private val teamsPayloadGenerator = TeamsPayloadGenerator()
    private val discordPayloadGenerator = DiscordPayloadGenerator()

    fun sendNotification(url: String, platform: WebhookPlatform, build: SRunningBuild, message: String) {
        val payload = when (platform) {
            WebhookPlatform.SLACK -> slackPayloadGenerator.generatePayload(build, message)
            WebhookPlatform.TEAMS -> teamsPayloadGenerator.generatePayload(build, message)
            WebhookPlatform.DISCORD -> discordPayloadGenerator.generatePayload(build, message)
        }
        LOG.info("Sending notification to $url (Platform: $platform): $payload")
        // TODO: Implement actual webhook sending logic using an HTTP client
    }
}