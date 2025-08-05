package sk.v2.plugins.teamnotify.payloads

import jetbrains.buildServer.serverSide.SRunningBuild

class DiscordPayloadGenerator : PayloadGenerator {
    override fun generatePayload(build: SRunningBuild, message: String): String {
        val buildTypeName = build.buildType?.name.orEmpty()
        val buildNumber = build.buildNumber.orEmpty()
        val buildExternalId = build.buildType?.externalId.orEmpty()
        return """{"content": "[$buildTypeName] $message - Build $buildNumber (<$buildExternalId|Open in TeamCity>)"}"""
    }
}
