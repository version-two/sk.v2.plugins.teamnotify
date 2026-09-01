package sk.v2.plugins.teamnotify.model

import org.jdom.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TeamNotifyProjectSettingsTest {

    @Test
    fun `webhook name survives a write-read round trip`() {
        val settings = TeamNotifyProjectSettings(
            mutableListOf(
                WebhookConfiguration("url1", WebhookPlatform.SLACK, name = "Release channel", onSuccess = true)
            )
        )

        val root = Element("settings")
        settings.writeTo(root)

        val restored = TeamNotifyProjectSettings()
        restored.readFrom(root)

        assertEquals(1, restored.webhooks.size)
        assertEquals("Release channel", restored.webhooks[0].name)
        assertEquals("url1", restored.webhooks[0].url)
    }

    @Test
    fun `webhook without a name reads back as null`() {
        val settings = TeamNotifyProjectSettings(
            mutableListOf(WebhookConfiguration("url1", WebhookPlatform.TEAMS, onFailure = true))
        )

        val root = Element("settings")
        settings.writeTo(root)

        val restored = TeamNotifyProjectSettings()
        restored.readFrom(root)

        assertEquals(1, restored.webhooks.size)
        assertNull(restored.webhooks[0].name)
    }

    @Test
    fun `build-config webhook names survive a write-read round trip`() {
        val settings = TeamNotifyProjectSettings(
            buildTypeWebhooks = mutableMapOf(
                "bt11" to mutableListOf(
                    WebhookConfiguration("url-bt", WebhookPlatform.DISCORD, name = "QA bot", onFailure = true)
                )
            )
        )

        val root = Element("settings")
        settings.writeTo(root)

        val restored = TeamNotifyProjectSettings()
        restored.readFrom(root)

        assertEquals("QA bot", restored.buildTypeWebhooks["bt11"]?.get(0)?.name)
    }
}
