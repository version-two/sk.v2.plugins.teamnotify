package sk.v2.plugins.teamnotify.validation

import org.junit.Test
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebhookUrlValidationTest {

    // Extracted validation logic for testing
    private fun isValidWebhookUrl(platform: WebhookPlatform, url: String): Boolean {
        val normalizedUrl = url.trim()

        if (normalizedUrl.isEmpty() || normalizedUrl.length > 2048) {
            return false
        }

        return when (platform) {
            WebhookPlatform.SLACK -> {
                Regex("^https://hooks\\.slack\\.com/(services|workflows)/[A-Z0-9]+/[A-Z0-9]+/[A-Za-z0-9_-]+.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
            WebhookPlatform.TEAMS -> {
                Regex("^https://[a-zA-Z0-9_-]+\\.webhook\\.office\\.com/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                Regex("^https://outlook\\.office\\.com/webhook/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                Regex("^https://[a-zA-Z0-9_-]+\\.logic\\.azure\\.com/.*", RegexOption.IGNORE_CASE).matches(normalizedUrl) ||
                Regex("^https://[a-zA-Z0-9_-]+\\.environment\\.api\\.powerplatform\\.com.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
            WebhookPlatform.DISCORD -> {
                Regex("^https://discord(?:app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+.*", RegexOption.IGNORE_CASE).matches(normalizedUrl)
            }
        }
    }

    // ========== SLACK TESTS ==========

    @Test
    fun `valid Slack services URL should pass`() {
        val url = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `valid Slack workflows URL should pass`() {
        val url = "https://hooks.slack.com/workflows/T00000000/A00000000/123456789012345678/abcdefghijklmnopqrst"
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with uppercase should pass (case insensitive)`() {
        val url = "https://HOOKS.SLACK.COM/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with query parameters should pass`() {
        val url = "https://hooks.slack.com/services/T00/B00/XXX?param=value"
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with HTTP (not HTTPS) should fail`() {
        val url = "http://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with wrong domain should fail`() {
        val url = "https://hooks.slack.net/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with wrong subdomain should fail`() {
        val url = "https://api.slack.com/webhooks/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with missing path should fail`() {
        val url = "https://hooks.slack.com"
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `Slack URL with incomplete path should fail`() {
        val url = "https://hooks.slack.com/services/T00"
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    // ========== TEAMS TESTS ==========

    @Test
    fun `valid Teams classic URL should pass`() {
        val url = "https://outlook.office.com/webhook/abc123-def456/IncomingWebhook/ghi789/jkl012"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams classic URL with GUIDs should pass`() {
        val url = "https://outlook.office.com/webhook/12345678-1234-1234-1234-123456789012/IncomingWebhook/67890123-4567-8901-2345-678901234567/98765432-1098-7654-3210-987654321098"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams standard tenant URL should pass`() {
        val url = "https://company.webhook.office.com/webhookb2/abc123/IncomingWebhook/def456/ghi789"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams standard URL with complex tenant should pass`() {
        val url = "https://myorg-123.webhook.office.com/webhookb2/12345678-1234-1234-1234-123456789012@98765432-4321-4321-4321-210987654321/IncomingWebhook/67890123-4567-8901-2345-678901234567/abcdefgh"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams Power Automate legacy URL should pass`() {
        val url = "https://prod-123.logic.azure.com/workflows/abcd1234efgh5678/triggers/manual/paths/invoke"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams Power Automate legacy URL with query params should pass`() {
        val url = "https://test-environment.logic.azure.com/workflows/12345678-1234-1234-1234-123456789012/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams Power Automate new URL should pass`() {
        val url = "https://default686ea1d3bc2b4c6fa92cd99c5c3016.35.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/abcd1234efgh5678/triggers/manual/paths/invoke"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `valid Teams Power Automate new URL without port should pass`() {
        val url = "https://prod123abc456def789.12.environment.api.powerplatform.com/powerautomate/automations/direct/workflows/12345678-1234-1234-1234-123456789012/triggers/manual/paths/invoke?api-version=2022-05-01"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams URL with uppercase should pass (case insensitive)`() {
        val url = "https://OUTLOOK.OFFICE.COM/webhook/abc123/IncomingWebhook/def456/ghi789"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams URL with HTTP (not HTTPS) should fail`() {
        val url = "http://outlook.office.com/webhook/abc123/IncomingWebhook/def456/ghi789"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams URL with wrong path (plural webhooks) should fail`() {
        val url = "https://outlook.office.com/webhooks/abc123/IncomingWebhook/def456/ghi789"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams URL with wrong domain should fail`() {
        val url = "https://outlook.office365.com/webhook/abc123/IncomingWebhook/def456/ghi789"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams tenant URL with wrong domain should fail`() {
        val url = "https://company.webhook.office365.com/webhookb2/abc123/IncomingWebhook/def456/ghi789"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams URL with missing path should fail`() {
        val url = "https://outlook.office.com"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `Teams tenant URL without subdomain should fail`() {
        val url = "https://webhook.office.com/webhookb2/abc123"
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    // ========== DISCORD TESTS ==========

    @Test
    fun `valid Discord URL should pass`() {
        val url = "https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `valid Discord URL with discordapp domain should pass`() {
        val url = "https://discordapp.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `valid Discord URL with long token should pass`() {
        val url = "https://discord.com/api/webhooks/987654321012345678/a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6A7B8C9D0E1F2"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with uppercase should pass (case insensitive)`() {
        val url = "https://DISCORD.COM/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with query parameters should pass`() {
        val url = "https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz?wait=true"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with HTTP (not HTTPS) should fail`() {
        val url = "http://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with wrong domain should fail`() {
        val url = "https://discord.gg/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL missing api path should fail`() {
        val url = "https://discord.com/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with singular webhook should fail`() {
        val url = "https://discord.com/api/webhook/1234567890/abcdefghijklmnopqrstuvwxyz"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with missing token should fail`() {
        val url = "https://discord.com/api/webhooks/1234567890"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with missing ID should fail`() {
        val url = "https://discord.com/api/webhooks"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    @Test
    fun `Discord URL with non-numeric ID should fail`() {
        val url = "https://discord.com/api/webhooks/abcdefghij/token123"
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, url))
    }

    // ========== EDGE CASES ==========

    @Test
    fun `empty URL should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, ""))
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, ""))
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, ""))
    }

    @Test
    fun `whitespace-only URL should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "   "))
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, "   "))
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, "   "))
    }

    @Test
    fun `URL exceeding 2048 characters should fail`() {
        val longUrl = "https://hooks.slack.com/services/" + "A".repeat(2050)
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, longUrl))
    }

    @Test
    fun `URL with leading and trailing whitespace should be trimmed and pass`() {
        val url = "  https://hooks.slack.com/services/T00/B00/XXX  "
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, url))
    }

    @Test
    fun `not a URL should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "not-a-url"))
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, "just-text"))
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, "123456"))
    }

    @Test
    fun `FTP protocol should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "ftp://hooks.slack.com/services/T00/B00/XXX"))
    }

    @Test
    fun `incomplete HTTPS URL should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "https://"))
        assertFalse(isValidWebhookUrl(WebhookPlatform.TEAMS, "https://"))
        assertFalse(isValidWebhookUrl(WebhookPlatform.DISCORD, "https://"))
    }

    @Test
    fun `URL with missing host should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "https:///path"))
    }

    @Test
    fun `XSS attempt should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "javascript:alert('xss')"))
    }

    @Test
    fun `URL with spaces in path should fail`() {
        assertFalse(isValidWebhookUrl(WebhookPlatform.SLACK, "https://hooks.slack.com/services/T00/B00/XXX YYY"))
    }

    @Test
    fun `URL with special characters should pass if encoded`() {
        val url = "https://outlook.office.com/webhook/abc%40def/IncomingWebhook/ghi/jkl"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, url))
    }

    @Test
    fun `URL with hyphens and underscores should pass`() {
        val slackUrl = "https://hooks.slack.com/services/T00/B00/XXX-YYY_ZZZ"
        assertTrue(isValidWebhookUrl(WebhookPlatform.SLACK, slackUrl))

        val teamsUrl = "https://my-org_123.webhook.office.com/webhookb2/abc-123_def/IncomingWebhook/ghi/jkl"
        assertTrue(isValidWebhookUrl(WebhookPlatform.TEAMS, teamsUrl))

        val discordUrl = "https://discord.com/api/webhooks/123456/abc-def_ghi"
        assertTrue(isValidWebhookUrl(WebhookPlatform.DISCORD, discordUrl))
    }
}
