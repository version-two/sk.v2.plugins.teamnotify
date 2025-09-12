package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import sk.v2.plugins.teamnotify.model.WebhookConfiguration
import java.util.Timer
import java.util.TimerTask

class StalledBuildTimer(
    private val buildStallTracker: BuildStallTracker,
    private val webhookManager: WebhookManager,
    private val webhookService: WebhookService,
    private val sBuildServer: SBuildServer
) : InitializingBean, DisposableBean {

    private val timer = Timer()

    override fun afterPropertiesSet() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                buildStallTracker.checkForStalledBuilds(300000) { buildId ->
                    sBuildServer.findRunningBuildById(buildId)?.let { build ->
                        val buildType = build.buildType ?: return@let
                        // Get webhooks from build type (includes inherited webhooks)
                        val webhooks = webhookManager.getWebhooksForBuildType(buildType)
                        for (webhook: WebhookConfiguration in webhooks) {
                            if (webhook.onStall) {
                                webhookService.sendNotification(webhook.url, webhook.platform, build, "Build stalled: ${build.buildType?.name.orEmpty()} #${build.buildNumber.orEmpty()}")
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