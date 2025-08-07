package sk.v2.plugins.teamnotify

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.ServerListener
import jetbrains.buildServer.serverSide.ServerListenerAdapter
import jetbrains.buildServer.util.EventDispatcher
import org.springframework.context.ApplicationContext
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContextAware

class TeamNotifyStartupActivity(
    private val eventDispatcher: EventDispatcher<ServerListener>
) : ServerListenerAdapter(), ApplicationContextAware {

    private val LOG = Logger.getInstance(TeamNotifyStartupActivity::class.java.name)
    private lateinit var applicationContext: ApplicationContext
    
    init {
        LOG.warn("TeamNotifyStartupActivity initialized")
        DebugLogger.log("TeamNotifyStartupActivity initialized")
        eventDispatcher.addListener(this)
    }
    
    override fun setApplicationContext(context: ApplicationContext) {
        LOG.warn("TeamNotifyStartupActivity.setApplicationContext called")
        DebugLogger.log("TeamNotifyStartupActivity.setApplicationContext called")
        this.applicationContext = context
    }
    
    override fun serverStartup() {
        LOG.warn("TeamNotifyStartupActivity.serverStartup called")
        DebugLogger.log("TeamNotifyStartupActivity.serverStartup called")
        try {
            // Verify admin page bean exists
            val adminPage = applicationContext.getBean("teamNotifyAdminPageExtension")
            LOG.warn("Found admin page bean: $adminPage")
            DebugLogger.log("Found admin page bean: $adminPage")
        } catch (e: BeansException) {
            LOG.warn("Failed to find admin page bean: ${e.message}", e)
            DebugLogger.log("Failed to find admin page bean: ${e.message}")
        }
        
        try {
            // Verify project tab bean exists
            val projectTab = applicationContext.getBean("projectWebhookSettingsTab")
            LOG.warn("Found project tab bean: $projectTab")
            DebugLogger.log("Found project tab bean: $projectTab")
        } catch (e: BeansException) {
            LOG.warn("Failed to find project tab bean: ${e.message}", e)
            DebugLogger.log("Failed to find project tab bean: ${e.message}")
        }
        
        try {
            // Verify build config tab bean exists
            val buildTypeTab = applicationContext.getBean("buildConfigurationWebhookSettingsTab")
            LOG.warn("Found build config tab bean: $buildTypeTab")
            DebugLogger.log("Found build config tab bean: $buildTypeTab")
        } catch (e: BeansException) {
            LOG.warn("Failed to find build config tab bean: ${e.message}", e)
            DebugLogger.log("Failed to find build config tab bean: ${e.message}")
        }
        
        LOG.warn("TeamNotify plugin bean verification complete")
        DebugLogger.log("TeamNotify plugin bean verification complete")
    }
}
