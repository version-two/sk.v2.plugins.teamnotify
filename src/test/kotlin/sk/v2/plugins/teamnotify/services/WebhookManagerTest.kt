package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.model.WebhookSource

class WebhookManagerTest {

    private lateinit var projectSettingsManager: ProjectSettingsManager
    private lateinit var sBuildServer: SBuildServer
    private lateinit var projectManager: ProjectManager
    private lateinit var webhookManager: WebhookManager
    private lateinit var project: SProject

    @BeforeEach
    fun setUp() {
        projectSettingsManager = mock(ProjectSettingsManager::class.java)
        sBuildServer = mock(SBuildServer::class.java)
        projectManager = mock(ProjectManager::class.java)
        `when`(sBuildServer.projectManager).thenReturn(projectManager)

        webhookManager = WebhookManager(projectSettingsManager, sBuildServer)
        project = mock(SProject::class.java)
        `when`(project.projectId).thenReturn("projectId")
        `when`(project.externalId).thenReturn("projectExternalId")
        `when`(project.name).thenReturn("Project Name")
    }

    @Test
    fun `getWebhooks returns empty list if no settings exist`() {
        val settings = TeamNotifyProjectSettings()
        `when`(projectSettingsManager.getSettings(eq("projectId"), anyString())).thenReturn(settings)

        val webhooks = webhookManager.getWebhooks(project)

        assert(webhooks.isEmpty())
    }

    @Test
    fun `getWebhooks returns list of webhooks if settings exist`() {
        val webhook1 = WebhookConfiguration("url1", WebhookPlatform.SLACK, onSuccess = true)
        val webhook2 = WebhookConfiguration("url2", WebhookPlatform.TEAMS, onFailure = true)
        val settings = TeamNotifyProjectSettings(mutableListOf(webhook1, webhook2))

        `when`(projectSettingsManager.getSettings(eq("projectId"), anyString())).thenReturn(settings)

        val webhooks = webhookManager.getWebhooks(project)

        assert(webhooks.size == 2)
        assert(webhooks[0] == webhook1)
        assert(webhooks[1] == webhook2)
    }

    @Test
    fun `saveWebhooks saves the list of webhooks and persists project`() {
        val webhook1 = WebhookConfiguration("url1", WebhookPlatform.SLACK, onSuccess = true)
        val webhook2 = WebhookConfiguration("url2", WebhookPlatform.TEAMS, onFailure = true)
        val webhookList = listOf(webhook1, webhook2)

        val settings = TeamNotifyProjectSettings()
        `when`(projectSettingsManager.getSettings(eq("projectId"), anyString())).thenReturn(settings)

        webhookManager.saveWebhooks(project, webhookList)

        assert(settings.webhooks == webhookList)
        verify(project, times(1)).persist()
    }

    @Test
    fun `saveWebhooksForBuildType stores under the single settings key without per-build-type registration`() {
        // Regression test for "corresponding factory was not registered": build-level webhooks
        // are stored in the per-build-type map of the project's single settings object, and no
        // settings factory is registered dynamically.
        val settings = TeamNotifyProjectSettings()
        `when`(projectSettingsManager.getSettings(eq("projectId"), anyString())).thenReturn(settings)

        val buildType = mock(SBuildType::class.java)
        `when`(buildType.buildTypeId).thenReturn("bt11")
        `when`(buildType.project).thenReturn(project)

        val webhook = WebhookConfiguration("url-bt", WebhookPlatform.SLACK, onSuccess = true)
        webhookManager.saveWebhooksForBuildType(buildType, listOf(webhook))

        assert(settings.buildTypeWebhooks["bt11"] == listOf(webhook))
        verify(project, times(1)).persist()
        verify(projectSettingsManager, never()).registerSettingsFactory(anyString(), any())
    }

    @Test
    fun `getAllWebhooks aggregates across projects`() {
        val p1 = mock(SProject::class.java)
        `when`(p1.projectId).thenReturn("p1Id")
        `when`(p1.externalId).thenReturn("P1")
        `when`(p1.name).thenReturn("Project One")
        val p2 = mock(SProject::class.java)
        `when`(p2.projectId).thenReturn("p2Id")
        `when`(p2.externalId).thenReturn("P2")
        `when`(p2.name).thenReturn("Project Two")

        `when`(projectManager.projects).thenReturn(listOf(p1, p2))

        val settings1 = TeamNotifyProjectSettings(mutableListOf(
            WebhookConfiguration("url1", WebhookPlatform.SLACK, onSuccess = true)
        ))
        val settings2 = TeamNotifyProjectSettings()

        `when`(projectSettingsManager.getSettings(eq("p1Id"), anyString())).thenReturn(settings1)
        `when`(projectSettingsManager.getSettings(eq("p2Id"), anyString())).thenReturn(settings2)

        val all = webhookManager.getAllWebhooks()
        assert(all.size == 1)
        assert(all[0].projectName == "Project One")
        assert(all[0].projectId == "P1")
        assert(all[0].webhook.url == "url1")
    }

    @Test
    fun `getInheritedWebhooksWithSourceForProject includes parent webhooks tagged PROJECT and excludes the project's own`() {
        // The project's own UI webhook must NOT appear (it is shown/edited in the editable list);
        // a webhook configured on the parent project must appear tagged as inherited (PROJECT).
        val ownWebhook = WebhookConfiguration("ownUrl", WebhookPlatform.SLACK, onSuccess = true)
        `when`(projectSettingsManager.getSettings(eq("projectId"), anyString()))
            .thenReturn(TeamNotifyProjectSettings(mutableListOf(ownWebhook)))

        val parent = mock(SProject::class.java)
        `when`(parent.projectId).thenReturn("parentId")
        `when`(parent.parentProject).thenReturn(null)
        val parentWebhook = WebhookConfiguration("parentUrl", WebhookPlatform.TEAMS, onFailure = true)
        `when`(projectSettingsManager.getSettings(eq("parentId"), anyString()))
            .thenReturn(TeamNotifyProjectSettings(mutableListOf(parentWebhook)))

        `when`(project.parentProject).thenReturn(parent)

        val result = webhookManager.getInheritedWebhooksWithSourceForProject(project)

        assert(result.size == 1)
        assert(result[0].webhook.url == "parentUrl")
        assert(result[0].source == WebhookSource.PROJECT)
    }
}