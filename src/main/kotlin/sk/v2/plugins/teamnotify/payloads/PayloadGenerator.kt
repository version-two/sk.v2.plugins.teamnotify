package sk.v2.plugins.teamnotify.payloads

interface PayloadGenerator {
    fun generatePayload(ctx: NotificationContext): String
}