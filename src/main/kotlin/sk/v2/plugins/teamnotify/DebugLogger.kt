package sk.v2.plugins.teamnotify

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple debug logger that writes directly to a file in TeamCity logs directory
 */
object DebugLogger {
    private val LOG_FILE = "/opt/teamcity/logs/teamnotify-debug.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    
    fun log(message: String) {
        try {
            val logFile = File(LOG_FILE)
            FileWriter(logFile, true).use { writer ->
                writer.write("[${dateFormat.format(Date())}] $message\n")
            }
        } catch (e: Exception) {
            // Can't do much if logging fails
        }
    }
}
