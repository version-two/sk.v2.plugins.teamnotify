package sk.v2.plugins.teamnotify.extensions

import jetbrains.buildServer.controllers.admin.AdminPage
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.PositionConstraint
import com.intellij.openapi.diagnostic.Logger
import sk.v2.plugins.teamnotify.DebugLogger
import javax.servlet.http.HttpServletRequest

class TeamNotifyAdminPageExtension(
    pagePlaces: PagePlaces,
    private val pluginDescriptor: PluginDescriptor
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

    override fun getGroup(): String {
        DebugLogger.log("TeamNotifyAdminPageExtension.getGroup called, returning: ${SERVER_RELATED_GROUP}")
        return SERVER_RELATED_GROUP
    }
}