package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import sk.v2.plugins.teamnotify.utils.BranchMatcher
import java.util.Timer
import java.util.TimerTask

class StalledBuildTimer(
    private val buildStallTracker: BuildStallTracker,
    private val webhookManager: WebhookManager,
    private val webhookService: WebhookService,
    private val sBuildServer: SBuildServer
) : InitializingBean, DisposableBean {

    // Daemon timer: if TeamCity unloads the plugin classloader before destroy() runs (e.g. a hot
    // reload), a non-daemon Timer thread would survive and pin the old classloader, leaking it.
    private val timer = Timer("teamnotify-stalled-build-timer", true)

    override fun afterPropertiesSet() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                buildStallTracker.checkForStalledBuilds(300000) { buildId ->
                    sBuildServer.findRunningBuildById(buildId)?.let { build ->
                        val buildType = build.buildType ?: return@let
                        val branchName = build.branch?.displayName
                        // Use effective webhooks so enabled/locally-disabled state is honored,
                        // and apply the branch filter, consistent with the other triggers.
                        val webhooks = webhookManager.getEffectiveWebhooksForBuildType(buildType)
                        for (webhook: WebhookConfiguration in webhooks) {
                            if (!BranchMatcher.matches(branchName, webhook.branchFilter)) {
                                continue
                            }
                            if (webhook.onStall) {
                                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build stalled: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}", webhook.includeChanges, webhook.authHeaderName, webhook.authHeaderValue, webhook.showBuildLink, webhook.showArtifacts)
                            }
                        }
                    }
                }
            }
        }, 60000, 60000)
    }

    override fun destroy() {
        timer.cancel()
    }
}