package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.services.WebhookManager
import javax.servlet.http.HttpServletRequest

class BuildConfigurationWebhookSettingsTab(
    @NotNull webControllerManager: WebControllerManager,
    @NotNull projectManager: ProjectManager,
    @NotNull private val pluginDescriptor: PluginDescriptor,
    @NotNull private val webhookManager: WebhookManager
) : BuildTypeTab(
    "webhookNotifier",
    "TeamNotify",
    webControllerManager,
    projectManager
) {
    private val LOG = Logger.getInstance(BuildConfigurationWebhookSettingsTab::class.java.name)

    init {
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("editNotifierSettings.jsp"))
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest, buildType: SBuildType, user: SUser?) {
        model["projectId"] = buildType.project.externalId
        model["buildTypeId"] = buildType.externalId
        model["project"] = buildType.project
        model["buildType"] = buildType
        
        // Check if versioned settings are enabled and in read-only mode
        val project = buildType.project
        val versionedSettingsMode = try {
            val settingsMethod = project.javaClass.getMethod("isVersionedSettingsEnabled")
            settingsMethod.invoke(project) as Boolean
        } catch (e: Exception) {
            false
        }
        
        val isReadOnly = try {
            if (versionedSettingsMode) {
                val editingMethod = project.javaClass.getMethod("isVersionedSettingsAllowUIEditing")
                !(editingMethod.invoke(project) as Boolean)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        
        model["versionedSettingsEnabled"] = versionedSettingsMode
        model["versionedSettingsReadOnly"] = isReadOnly
        
        try {
            // Get webhooks specific to this build type (doesn't include parent webhooks for display)
            val buildTypeKey = "teamnotify.settings.${buildType.buildTypeId}"
            val hooks = try {
                val settings = webhookManager.getWebhooksForEntity(null, buildType.externalId)
                settings.filter { 
                    // Only show webhooks directly configured for this build type, not inherited ones
                    true // For now show all, but you could filter here if needed
                }
            } catch (e: Exception) {
                emptyList()
            }
            model["webhooks"] = hooks
        } catch (e: Exception) {
            LOG.warn("Failed to load webhooks for buildType ${buildType.externalId}: ${e.message}")
        }
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        return true
    }
}