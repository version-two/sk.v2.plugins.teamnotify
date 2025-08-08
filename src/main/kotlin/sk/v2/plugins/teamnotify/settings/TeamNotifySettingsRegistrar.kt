package sk.v2.plugins.teamnotify.settings

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager

class TeamNotifySettingsRegistrar(
    private val projectSettingsManager: ProjectSettingsManager,
    private val teamNotifySettingsFactory: TeamNotifySettingsFactory
) {
    fun register() {
        // Register the settings factory for our settings key so that getSettings/getOrCreate works
        projectSettingsManager.registerSettingsFactory("teamnotify.settings", teamNotifySettingsFactory)
    }
}
