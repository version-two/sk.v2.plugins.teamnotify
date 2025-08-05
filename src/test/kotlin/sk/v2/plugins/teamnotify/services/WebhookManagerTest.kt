package sk.v2.plugins.teamnotify.services

import com.google.gson.Gson
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import sk.v2.plugins.teamnotify.model.Webhook
import sk.v2.plugins.teamnotify.model.WebhookPlatform

class WebhookManagerTest {

    private lateinit var projectSettingsManager: ProjectSettingsManager
    private lateinit var webhookManager: WebhookManager

    @BeforeEach
    fun setUp() {
        projectSettingsManager = mock(ProjectSettingsManager::class.java)
        webhookManager = WebhookManager(projectSettingsManager)
    }

    @Test
    fun `getWebhooks returns empty list if no settings exist`() {
        `when`(projectSettingsManager.getSettings(anyString(), anyString())).thenReturn(mock(ProjectSettingsManager.ProjectSettings::class.java))
        `when`(projectSettingsManager.getSettings(anyString(), anyString()).readText()).thenReturn("")

        val webhooks = webhookManager.getWebhooks("testProjectId")

        assert(webhooks.isEmpty())
    }

    @Test
    fun `getWebhooks returns list of webhooks if settings exist`() {
        val webhook1 = Webhook("url1", WebhookPlatform.SLACK, onSuccess = true)
        val webhook2 = Webhook("url2", WebhookPlatform.TEAMS, onFailure = true)
        val webhookList = listOf(webhook1, webhook2)
        val json = Gson().toJson(webhookList)

        `when`(projectSettingsManager.getSettings(anyString(), anyString())).thenReturn(mock(ProjectSettingsManager.ProjectSettings::class.java))
        `when`(projectSettingsManager.getSettings(anyString(), anyString()).readText()).thenReturn(json)

        val webhooks = webhookManager.getWebhooks("testProjectId")

        assert(webhooks.size == 2)
        assert(webhooks[0] == webhook1)
        assert(webhooks[1] == webhook2)
    }

    @Test
    fun `saveWebhooks saves the list of webhooks`() {
        val webhook1 = Webhook("url1", WebhookPlatform.SLACK, onSuccess = true)
        val webhook2 = Webhook("url2", WebhookPlatform.TEAMS, onFailure = true)
        val webhookList = listOf(webhook1, webhook2)

        val projectSettings = mock(ProjectSettingsManager.ProjectSettings::class.java)
        `when`(projectSettingsManager.getSettings(anyString(), anyString())).thenReturn(projectSettings)

        webhookManager.saveWebhooks("testProjectId", webhookList)

        verify(projectSettings).writeText(Gson().toJson(webhookList))
        verify(projectSettingsManager).persistSettings("testProjectId", "webhooks")
    }
}