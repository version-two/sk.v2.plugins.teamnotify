package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.controllers.admin.AdminPage
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.PositionConstraint
import org.jetbrains.annotations.NotNull
import sk.v2.plugins.teamnotify.services.WebhookManager
import javax.servlet.http.HttpServletRequest

class TeamNotifyAdminPageExtension(
    @NotNull pagePlaces: PagePlaces,
    @NotNull private val pluginDescriptor: PluginDescriptor,
    @NotNull private val webhookManager: WebhookManager
) : AdminPage(pagePlaces) {
    init {
        setPluginName("teamNotify")
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("teamNotifyAdmin.jsp"))
        setTabTitle("TeamNotify")
        addCssFile(pluginDescriptor.getPluginResourcesPath("css/admin/adminMain.css"))
        setPosition(PositionConstraint.after("plugins"))
        register()
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        return super.isAvailable(request)
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        val allWebhooks = webhookManager.getAllWebhooks()
        model["allWebhooks"] = allWebhooks
        model["totalWebhooks"] = allWebhooks.size
    }

    override fun getGroup(): String {
        return SERVER_RELATED_GROUP
    }
}