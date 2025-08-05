package sk.v2.plugins.teamnotify.services

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.SRunningBuild
import java.util.concurrent.ConcurrentHashMap

class BuildStallTracker {

    private val LOG = Logger.getInstance(BuildStallTracker::class.java.name)
    private val runningBuilds = ConcurrentHashMap<Long, Long>()

    fun startTracking(build: SRunningBuild) {
        runningBuilds[build.buildId] = System.currentTimeMillis()
        LOG.info("Started tracking build ${build.buildId}")
    }

    fun stopTracking(build: SRunningBuild) {
        runningBuilds.remove(build.buildId)
        LOG.info("Stopped tracking build ${build.buildId}")
    }

    fun checkForStalledBuilds(stallTimeout: Long, action: (Long) -> Unit) {
        val now = System.currentTimeMillis()
        for ((buildId, lastActivity) in runningBuilds) {
            if (now - lastActivity > stallTimeout) {
                LOG.info("Build $buildId has stalled")
                action(buildId)
                runningBuilds.remove(buildId) // Prevent multiple notifications
            }
        }
    }
}