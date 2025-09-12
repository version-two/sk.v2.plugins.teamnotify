package sk.v2.plugins.teamnotify.settings

import jetbrains.buildServer.serverSide.settings.ProjectSettings
import jetbrains.buildServer.serverSide.settings.ProjectSettingsFactory
import sk.v2.plugins.teamnotify.model.TeamNotifyProjectSettings

class TeamNotifySettingsFactory : ProjectSettingsFactory {
    override fun createProjectSettings(projectId: String): ProjectSettings {
        return TeamNotifyProjectSettings()
    }
}

class DisabledWebhooksSettingsFactory : ProjectSettingsFactory {
    override fun createProjectSettings(projectId: String): ProjectSettings {
        return DisabledWebhooksSettings()
    }
}