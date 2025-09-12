package sk.v2.plugins.teamnotify.controllers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.model.WebhookPlatform
import sk.v2.plugins.teamnotify.services.WebhookManager
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BackupRestoreController(
    private val webControllerManager: WebControllerManager,
    private val webhookManager: WebhookManager,
    private val sBuildServer: SBuildServer
) : BaseController() {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun register() {
        webControllerManager.registerController("/admin/teamnotify/backup.html", this)
        webControllerManager.registerController("/admin/teamnotify/restore.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val path = request.requestURI ?: ""
        
        when {
            path.endsWith("/backup.html") -> handleBackup(response)
            path.endsWith("/restore.html") -> handleRestore(request, response)
        }
        
        return null
    }
    
    private fun handleBackup(response: HttpServletResponse) {
        try {
            // Collect all webhooks with their project/build type associations
            val allWebhooks = webhookManager.getAllWebhooks()
            
            // Create backup data structure
            val backupData = BackupData(
                version = "1.0",
                timestamp = System.currentTimeMillis(),
                serverUrl = sBuildServer.rootUrl,
                webhooks = allWebhooks.map { webhookInfo ->
                    WebhookBackupEntry(
                        projectId = webhookInfo.projectId,
                        projectName = webhookInfo.projectName,
                        buildTypeId = webhookInfo.buildTypeId,
                        buildTypeName = webhookInfo.buildTypeName,
                        webhook = webhookInfo.webhook
                    )
                }
            )
            
            // Generate filename with timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
            val timestamp = dateFormat.format(Date())
            val filename = "teamnotify_backup_${timestamp}.json"
            
            // Set response headers for file download
            response.contentType = "application/json"
            response.setHeader("Content-Disposition", "attachment; filename=\"$filename\"")
            
            // Write JSON to response
            response.writer.write(gson.toJson(backupData))
            response.writer.flush()
            
        } catch (e: Exception) {
            response.status = 500
            response.contentType = "application/json"
            response.writer.write("""{"success":false,"error":"${e.message}"}""")
        }
    }
    
    private fun handleRestore(request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        
        try {
            // Read the uploaded JSON
            val jsonContent = request.reader.readText()
            if (jsonContent.isBlank()) {
                response.status = 400
                response.writer.write("""{"success":false,"error":"No data provided"}""")
                return
            }
            
            // Parse the backup data
            val backupData = gson.fromJson(jsonContent, BackupData::class.java)
            
            // Validate backup data
            if (backupData.webhooks.isEmpty()) {
                response.status = 400
                response.writer.write("""{"success":false,"error":"No webhooks found in backup"}""")
                return
            }
            
            var restoredCount = 0
            var skippedCount = 0
            val errors = mutableListOf<String>()
            
            // Group webhooks by project/build type
            val webhooksByEntity = backupData.webhooks.groupBy { 
                "${it.projectId ?: ""}|${it.buildTypeId ?: ""}"
            }
            
            // Restore webhooks for each entity
            webhooksByEntity.forEach { (entityKey, entries) ->
                val parts = entityKey.split("|")
                val projectId = parts[0].takeIf { it.isNotEmpty() }
                val buildTypeId = parts[1].takeIf { it.isNotEmpty() }
                
                // Check if project/build type exists
                val project = when {
                    buildTypeId != null -> sBuildServer.projectManager.findBuildTypeByExternalId(buildTypeId)?.project
                    projectId != null -> sBuildServer.projectManager.findProjectByExternalId(projectId)
                    else -> null
                }
                
                if (project == null) {
                    skippedCount += entries.size
                    errors.add("Project/BuildType not found: ${projectId ?: buildTypeId}")
                    return@forEach
                }
                
                try {
                    // Get existing webhooks to avoid duplicates
                    val existingWebhooks = webhookManager.getWebhooksForEntity(projectId, buildTypeId)
                    val existingUrls = existingWebhooks.map { it.url }.toSet()
                    
                    // Add non-duplicate webhooks
                    val newWebhooks = entries.mapNotNull { entry ->
                        if (entry.webhook.url !in existingUrls) {
                            restoredCount++
                            entry.webhook
                        } else {
                            skippedCount++
                            null
                        }
                    }
                    
                    if (newWebhooks.isNotEmpty()) {
                        val allWebhooks = existingWebhooks + newWebhooks
                        webhookManager.saveWebhooksForEntity(projectId, buildTypeId, allWebhooks)
                    }
                    
                } catch (e: Exception) {
                    errors.add("Error restoring to ${projectId ?: buildTypeId}: ${e.message}")
                }
            }
            
            // Return response
            val responseData = mutableMapOf<String, Any>(
                "success" to true,
                "restored" to restoredCount,
                "skipped" to skippedCount
            )
            
            if (errors.isNotEmpty()) {
                responseData["warnings"] = errors
            }
            
            response.writer.write(gson.toJson(responseData))
            
        } catch (e: Exception) {
            response.status = 500
            response.writer.write("""{"success":false,"error":"Failed to restore: ${e.message}"}""")
        }
    }
    
    // Data classes for backup/restore
    data class BackupData(
        val version: String,
        val timestamp: Long,
        val serverUrl: String?,
        val webhooks: List<WebhookBackupEntry>
    )
    
    data class WebhookBackupEntry(
        val projectId: String?,
        val projectName: String?,
        val buildTypeId: String?,
        val buildTypeName: String?,
        val webhook: WebhookConfiguration
    )
}