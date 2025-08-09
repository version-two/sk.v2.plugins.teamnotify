package sk.v2.plugins.teamnotify

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Legacy startup hook used during development. Kept as a no-op for compatibility.
 */
class TeamNotifyStartupActivity : ApplicationContextAware {
    override fun setApplicationContext(context: ApplicationContext) {
        // no-op
    }
}
