package sk.v2.plugins.teamnotify.listeners

import jetbrains.buildServer.serverSide.BuildServerAdapter
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.SFinishedBuild
import jetbrains.buildServer.serverSide.SRunningBuild
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.services.BuildDurationService
import sk.v2.plugins.teamnotify.services.BuildStallTracker
import sk.v2.plugins.teamnotify.services.WebhookManager
import sk.v2.plugins.teamnotify.services.WebhookService
import sk.v2.plugins.teamnotify.utils.BranchMatcher
import org.springframework.beans.factory.DisposableBean

class NotifierBuildServerListener(
    private val sBuildServer: SBuildServer,
    private val webhookService: WebhookService,
    private val webhookManager: WebhookManager,
    private val buildStallTracker: BuildStallTracker,
    private val buildDurationService: BuildDurationService
) : BuildServerAdapter(), DisposableBean {

    fun register() {
        sBuildServer.addListener(this)
    }
    
    override fun destroy() {
        sBuildServer.removeListener(this)
    }

    override fun buildStarted(build: SRunningBuild) {
        buildStallTracker.startTracking(build)
        val buildType = build.buildType ?: return
        val branchName = build.branch?.displayName
        
        // Get webhooks from build configuration level and all parent projects
        val webhooks = webhookManager.getWebhooksForBuildType(buildType)
        for (webhook in webhooks) {
            // Check branch filter
            if (!BranchMatcher.matches(branchName, webhook.branchFilter)) {
                continue
            }
            
            if (webhook.onStart) {
                webhookService.sendNotification(
                    webhook.url,
                    webhook.platform,
                    build,
                    "Build started: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}",
                    webhook.includeChanges
                )
            }
        }
    }

    override fun buildFinished(build: SRunningBuild) {
        buildStallTracker.stopTracking(build)
        val buildType = build.buildType ?: return
        val branchName = build.branch?.displayName
        
        // Get webhooks from build configuration level and all parent projects
        val webhooks = webhookManager.getWebhooksForBuildType(buildType)

        for (webhook in webhooks) {
            // Check branch filter
            if (!BranchMatcher.matches(branchName, webhook.branchFilter)) {
                continue
            }
            // On Success
            if (build.buildStatus.isSuccessful && webhook.onSuccess) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build successful: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
            }

            // On Failure
            if (!build.buildStatus.isSuccessful && webhook.onFailure) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build failed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
            }

            // Build Longer Than
            webhook.buildLongerThan?.let {
                if (build.duration > it) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than $it seconds: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
                }
            }

            // Build Longer Than Average
            if (webhook.buildLongerThanAverage) {
                val averageDuration = buildDurationService.getAverageBuildDuration(build.buildTypeId!!)
                if (averageDuration > 0 && build.duration > averageDuration) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than average: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
                }
            }

            // On Build Fixed / On First Failure
            // Get the previous finished build for this build type
            val buildType = build.buildType
            val previousFinishedBuild = if (buildType != null) {
                buildType.getHistory().firstOrNull { finishedBuild -> finishedBuild.buildId != build.buildId }
            } else null

            if (previousFinishedBuild != null) {
                val currentBuildSuccessful = build.buildStatus.isSuccessful
                val previousBuildSuccessful = previousFinishedBuild.buildStatus.isSuccessful

                // On Build Fixed
                if (webhook.onBuildFixed && currentBuildSuccessful && !previousBuildSuccessful) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build fixed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
                }
                // On First Failure
                if (webhook.onFirstFailure && !currentBuildSuccessful && previousBuildSuccessful) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "First failure: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges)
                }
            }
        }
    }
}