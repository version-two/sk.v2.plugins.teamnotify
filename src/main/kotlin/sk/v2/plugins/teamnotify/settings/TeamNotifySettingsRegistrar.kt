package sk.v2.plugins.teamnotify.settings

import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager

class TeamNotifySettingsRegistrar(
    private val projectSettingsManager: ProjectSettingsManager,
    private val teamNotifySettingsFactory: TeamNotifySettingsFactory,
    private val disabledWebhooksSettingsFactory: DisabledWebhooksSettingsFactory
) {
    companion object {
        // Use the plugin name as defined in teamcity-plugin.xml
        private const val PLUGIN_NAME = "team-notify"
        const val SETTINGS_KEY = "$PLUGIN_NAME.settings"
    }
    
    fun register() {
        // Register the settings factory for our main settings key
        projectSettingsManager.registerSettingsFactory(SETTINGS_KEY, teamNotifySettingsFactory)
        
        // Register factory for build type specific webhooks
        // Pattern matches "team-notify.settings.{buildTypeId}"
        projectSettingsManager.registerSettingsFactory("$SETTINGS_KEY.*", teamNotifySettingsFactory)
        
        // Register factories for build type specific disabled webhooks
        // Pattern matches "team-notify.settings.disabled.{buildTypeId}"
        projectSettingsManager.registerSettingsFactory("$SETTINGS_KEY.disabled.*", disabledWebhooksSettingsFactory)
    }
}
