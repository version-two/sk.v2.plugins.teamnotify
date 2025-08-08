package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.controllers.admin.AdminPage
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.PositionConstraint
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.DebugLogger
import sk.v2.plugins.teamnotify.services.WebhookManager
import javax.servlet.http.HttpServletRequest

class TeamNotifyAdminPageExtension(
    @NotNull pagePlaces: PagePlaces,
    @NotNull private val pluginDescriptor: PluginDescriptor,
    @NotNull private val webhookManager: WebhookManager
) : AdminPage(pagePlaces) {
    
    private val LOG = Logger.getInstance(TeamNotifyAdminPageExtension::class.java.name)

    init {
        DebugLogger.log("TeamNotifyAdminPageExtension is being initialized")
        setPluginName("teamNotify")
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("teamNotifyAdmin.jsp"))
        setTabTitle("TeamNotify")
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
        setPosition(PositionConstraint.after("plugins"))
        DebugLogger.log("About to register TeamNotifyAdminPageExtension")
        register()
        DebugLogger.log("TeamNotifyAdminPageExtension has been registered")
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        val available = super.isAvailable(request)
        DebugLogger.log("TeamNotifyAdminPageExtension.isAvailable called, returning: $available")
        return available
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        // Supply data expected by teamNotifyAdmin.jsp
        val allWebhooks = webhookManager.getAllWebhooks()
        model["allWebhooks"] = allWebhooks
        model["totalWebhooks"] = allWebhooks.size
        DebugLogger.log("TeamNotifyAdminPageExtension.fillModel populated model: totalWebhooks=${allWebhooks.size}")
    }

    override fun getGroup(): String {
        DebugLogger.log("TeamNotifyAdminPageExtension.getGroup called, returning: ${SERVER_RELATED_GROUP}")
        return SERVER_RELATED_GROUP
    }
}