package sk.v2.plugins.teamnotify

import com.intellij.openapi.diagnostic.Logger
import org.springframework.context.ApplicationContext
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class TeamNotifyStartupActivity : ApplicationContextAware {

    private val LOG = Logger.getInstance(TeamNotifyStartupActivity::class.java.name)
    private lateinit var applicationContext: ApplicationContext
    
    override fun setApplicationContext(context: ApplicationContext) {
        try {
            LOG.warn("!!!!! TeamNotifyStartupActivity.setApplicationContext called !!!!!")
            DebugLogger.log("TeamNotifyStartupActivity.setApplicationContext called")
            this.applicationContext = context
            
            // Immediately check for our beans
            verifyBeans()
        } catch (e: Exception) {
            LOG.warn("!!!!! TeamNotifyStartupActivity.setApplicationContext FAILED: ${e.message} !!!!!")
            DebugLogger.log("TeamNotifyStartupActivity.setApplicationContext FAILED: ${e.message}")
        }
    }
    
    @PostConstruct
    fun init() {
        try {
            LOG.warn("!!!!! TeamNotifyStartupActivity @PostConstruct init called !!!!!")
            DebugLogger.log("TeamNotifyStartupActivity @PostConstruct init called")
            verifyBeans()
            LOG.warn("!!!!! TeamNotify plugin bean verification complete !!!!!")
            DebugLogger.log("TeamNotify plugin bean verification complete")
        } catch (e: Exception) {
            LOG.warn("!!!!! TeamNotifyStartupActivity @PostConstruct FAILED: ${e.message} !!!!!")
            DebugLogger.log("!!!!! TeamNotifyStartupActivity @PostConstruct FAILED: ${e.message}")
        }
    }
    
    private fun verifyBeans() {
        try {
            LOG.warn("!!!!! TeamNotifyStartupActivity checking for admin page bean !!!!!")
            // Verify admin page bean exists
            val adminPage = applicationContext.getBean("teamNotifyAdminPageExtension")
            LOG.warn("!!!!! Found admin page bean: $adminPage !!!!!")
            DebugLogger.log("Found admin page bean: $adminPage")
        } catch (e: BeansException) {
            LOG.warn("!!!!! Failed to find admin page bean: ${e.message} !!!!!")
            DebugLogger.log("Failed to find admin page bean: ${e.message}")
        }
        
        try {
            LOG.warn("!!!!! TeamNotifyStartupActivity checking for project tab bean !!!!!")
            // Verify project tab bean exists
            val projectTab = applicationContext.getBean("projectWebhookSettingsTab")
            LOG.warn("!!!!! Found project tab bean: $projectTab !!!!!")
            DebugLogger.log("Found project tab bean: $projectTab")
        } catch (e: BeansException) {
            LOG.warn("!!!!! Failed to find project tab bean: ${e.message} !!!!!")
            DebugLogger.log("Failed to find project tab bean: ${e.message}")
        }
        
        try {
            LOG.warn("!!!!! TeamNotifyStartupActivity checking for build config tab bean !!!!!")
            // Verify build config tab bean exists
            val buildTypeTab = applicationContext.getBean("buildConfigurationWebhookSettingsTab")
            LOG.warn("!!!!! Found build config tab bean: $buildTypeTab !!!!!")
            DebugLogger.log("Found build config tab bean: $buildTypeTab")
        } catch (e: BeansException) {
            LOG.warn("!!!!! Failed to find build config tab bean: ${e.message} !!!!!")
            DebugLogger.log("Failed to find build config tab bean: ${e.message}")
        }
    }
}
