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

class NotifierBuildServerListener(
    private val sBuildServer: SBuildServer,
    private val webhookService: WebhookService,
    private val webhookManager: WebhookManager,
    private val buildStallTracker: BuildStallTracker,
    private val buildDurationService: BuildDurationService
) : BuildServerAdapter() {

    fun register() {
        sBuildServer.addListener(this)
    }

    override fun buildStarted(build: SRunningBuild) {
        buildStallTracker.startTracking(build)
    }

    override fun buildFinished(build: SRunningBuild) {
        buildStallTracker.stopTracking(build)
        val project = build.buildType?.project ?: return
        val webhooks = webhookManager.getWebhooks(project)

        for (webhook in webhooks) {
            // On Success
            if (build.buildStatus.isSuccessful && webhook.onSuccess) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build successful: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
            }

            // On Failure
            if (!build.buildStatus.isSuccessful && webhook.onFailure) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build failed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
            }

            // Build Longer Than
            webhook.buildLongerThan?.let {
                if (build.duration > it) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than $it seconds: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
                }
            }

            // Build Longer Than Average
            if (webhook.buildLongerThanAverage) {
                val averageDuration = buildDurationService.getAverageBuildDuration(build.buildTypeId!!)
                if (averageDuration > 0 && build.duration > averageDuration) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than average: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
                }
            }

            // On Build Fixed / On First Failure
            // Get the previous finished build for this build type
            val buildType = build.buildType
            val previousFinishedBuild = if (buildType != null) {
                buildType.getHistory().firstOrNull { finishedBuild -> finishedBuild.buildId != build.buildId }
            } else null

            if (previousFinishedBuild != null) {
                println("Type of webhook.onBuildFixed: ${webhook.onBuildFixed::class.simpleName}")
                println("Type of build.buildStatus.isSuccessful: ${build.buildStatus.isSuccessful::class.simpleName}")
                println("Type of previousFinishedBuild status: ${previousFinishedBuild::class.simpleName}")

                val currentBuildSuccessful = build.buildStatus.isSuccessful
                val previousBuildSuccessful = previousFinishedBuild.buildStatus.isSuccessful

                // On Build Fixed
                if (webhook.onBuildFixed && currentBuildSuccessful && !previousBuildSuccessful) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build fixed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
                }
                // On First Failure
                if (webhook.onFirstFailure && !currentBuildSuccessful && previousBuildSuccessful) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "First failure: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
                }
            }
        }
    }
}