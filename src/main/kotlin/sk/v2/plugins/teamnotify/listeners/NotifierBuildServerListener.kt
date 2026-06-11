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
        val webhooks = webhookManager.getEffectiveWebhooksForBuildType(buildType)
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
                    webhook.includeChanges,
                    webhook.authHeaderName,
                    webhook.authHeaderValue,
                    webhook.showBuildLink,
                    webhook.showArtifacts
                )
            }
        }
    }

    override fun buildInterrupted(build: SRunningBuild) {
        buildStallTracker.stopTracking(build)
        val buildType = build.buildType ?: return
        val branchName = build.branch?.displayName
        
        // Get webhooks from build configuration level and all parent projects
        val webhooks = webhookManager.getEffectiveWebhooksForBuildType(buildType)
        for (webhook in webhooks) {
            // Check branch filter
            if (!BranchMatcher.matches(branchName, webhook.branchFilter)) {
                continue
            }
            
            if (webhook.onCancel) {
                webhookService.sendNotification(
                    webhook.url,
                    webhook.platform,
                    build,
                    "Build cancelled: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}",
                    webhook.includeChanges,
                    webhook.authHeaderName,
                    webhook.authHeaderValue,
                    webhook.showBuildLink,
                    webhook.showArtifacts
                )
            }
        }
    }

    override fun buildFinished(build: SRunningBuild) {
        buildStallTracker.stopTracking(build)
        val buildType = build.buildType ?: return
        val branchName = build.branch?.displayName
        
        // Get webhooks from build configuration level and all parent projects
        val webhooks = webhookManager.getEffectiveWebhooksForBuildType(buildType)

        val currentBuildSuccessful = build.buildStatus.isSuccessful

        // Determine first-failure / fixed status once for the build (it does not vary per webhook).
        // Get the previous finished build for this build type, restricted to the SAME branch: build
        // history is cross-branch, so without this filter a green build on `main` would be treated as
        // "fixing" a red build on a feature branch (and vice versa), producing bogus notifications.
        val previousFinishedBuild = buildType.getHistory().firstOrNull { finishedBuild ->
            finishedBuild.buildId != build.buildId &&
                !finishedBuild.isPersonal &&
                finishedBuild.canceledInfo == null &&
                finishedBuild.branch?.displayName == branchName
        }
        val previousBuildSuccessful = previousFinishedBuild?.buildStatus?.isSuccessful
        val isBuildFixed = previousBuildSuccessful == false && currentBuildSuccessful
        val isFirstFailure = previousBuildSuccessful == true && !currentBuildSuccessful

        for (webhook in webhooks) {
            // Check branch filter
            if (!BranchMatcher.matches(branchName, webhook.branchFilter)) {
                continue
            }
            // On Success. Suppressed when this is a "fixed" build and the more specific onBuildFixed
            // trigger is enabled, so a single fixed build does not fire two notifications.
            if (currentBuildSuccessful && webhook.onSuccess && !(isBuildFixed && webhook.onBuildFixed)) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build successful: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
            }

            // On Failure. Suppressed when this is a "first failure" and the more specific
            // onFirstFailure trigger is enabled, to avoid a duplicate notification for the same event.
            if (!currentBuildSuccessful && webhook.onFailure && !(isFirstFailure && webhook.onFirstFailure)) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build failed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
            }

            // Build Longer Than
            webhook.buildLongerThan?.let {
                if (build.duration > it) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than $it seconds: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
                }
            }

            // Build Longer Than Average
            if (webhook.buildLongerThanAverage) {
                val averageDuration = buildDurationService.getAverageBuildDuration(build.buildTypeId)
                if (averageDuration > 0 && build.duration > averageDuration) {
                    webhookService.sendNotification(webhook.url, webhook.platform, build, "Build took longer than average: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
                }
            }

            // On Build Fixed
            if (webhook.onBuildFixed && isBuildFixed) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build fixed: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
            }
            // On First Failure
            if (webhook.onFirstFailure && isFirstFailure) {
                webhookService.sendNotification(webhook.url, webhook.platform, build, "First failure: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
            }
        }
    }
}