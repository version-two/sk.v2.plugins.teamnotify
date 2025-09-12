package sk.v2.plugins.teamnotify.payloads

import jetbrains.buildServer.serverSide.SRunningBuild
import java.util.*

enum class NotificationStatus {
    STARTED,
    SUCCESS,
    FAILURE,
    STALLED,
    CANCELLED,
    FIXED,
    FIRST_FAILURE,
    LONGER_THAN,
    LONGER_THAN_AVERAGE
}

data class NotificationContext(
    val status: NotificationStatus,
    val build: SRunningBuild,
    val title: String,
    val message: String,
    val rootUrl: String?,
    val buildUrl: String?,
    val artifactsUrl: String?,
    val projectName: String?,
    val buildTypeName: String?,
    val buildExternalId: String?,
    val buildNumber: String?,
    val triggeredBy: String?,
    val agentName: String?,
    val startTime: Date?,
    val finishTime: Date?,
    val changes: List<ChangeSummary> = emptyList(),
    val artifacts: List<ArtifactSummary> = emptyList()
)

data class ChangeSummary(
    val version: String?,
    val user: String?,
    val comment: String?
)

data class ArtifactSummary(
    val name: String,
    val path: String,
    val size: Long? = null,
    val downloadUrl: String
)
