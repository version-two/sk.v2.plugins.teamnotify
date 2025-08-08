package sk.v2.plugins.teamnotify

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.intellij.openapi.diagnostic.Logger

/**
 * Simple debug logger that writes directly to a file in TeamCity data directory
 */
object DebugLogger {
    // Use /data/teamcity_server/datadir/ which should be writable
    private val LOG_FILE = "/data/teamcity_server/datadir/teamnotify-debug.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private val LOG = Logger.getInstance(DebugLogger::class.java.name)
    
    init {
        // Send a clear message to regular logs as well
        LOG.warn("!!!!! TEAMNOTIFY-DEBUG-LOGGER INITIALIZED !!!!!")
    }
    
    fun log(message: String) {
        try {
            // Log to standard TeamCity logs first
            LOG.warn("TEAMNOTIFY-DEBUG: $message")
            
            // Also write to our direct file
            val logFile = File(LOG_FILE)
            FileWriter(logFile, true).use { writer ->
                writer.write("[${dateFormat.format(Date())}] $message\n")
            }
        } catch (e: Exception) {
            // If direct file logging fails, at least try standard logging
            LOG.warn("TEAMNOTIFY-DEBUG-ERROR: Failed to write to debug log file: ${e.message}")
        }
    }
}
