package sk.v2.plugins.teamnotify.settings

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager

class TeamNotifySettingsRegistrar(
    private val projectSettingsManager: ProjectSettingsManager,
    private val teamNotifySettingsFactory: TeamNotifySettingsFactory
) {
    companion object {
        // Use the plugin name as defined in teamcity-plugin.xml
        private const val PLUGIN_NAME = "team-notify"
        const val SETTINGS_KEY = "$PLUGIN_NAME.settings"
    }
    
    fun register() {
        // Register the settings factory for our main settings key (project-level webhooks)
        // Build-type specific keys are registered dynamically by WebhookManager
        projectSettingsManager.registerSettingsFactory(SETTINGS_KEY, teamNotifySettingsFactory)
    }
}
