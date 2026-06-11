package sk.v2.plugins.teamnotify.services

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.SRunningBuild
import java.util.concurrent.ConcurrentHashMap

class BuildStallTracker {

    private val LOG = Logger.getInstance(BuildStallTracker::class.java.name)
    private val runningBuilds = ConcurrentHashMap<Long, Long>()

    // Time source, overridable in tests so stall detection can be exercised deterministically
    // instead of depending on real elapsed wall-clock time.
    internal var clock: () -> Long = { System.currentTimeMillis() }

    fun startTracking(build: SRunningBuild) {
        runningBuilds[build.buildId] = clock()
        LOG.info("Started tracking build ${build.buildId}")
    }

    fun stopTracking(build: SRunningBuild) {
        runningBuilds.remove(build.buildId)
        LOG.info("Stopped tracking build ${build.buildId}")
    }

    fun checkForStalledBuilds(stallTimeout: Long, action: (Long) -> Unit) {
        val now = clock()
        val stalledBuilds = mutableListOf<Long>()

        // Collect stalled builds first
        for ((buildId, lastActivity) in runningBuilds) {
            if (now - lastActivity > stallTimeout) {
                stalledBuilds.add(buildId)
            }
        }
        
        // Process and remove stalled builds. Guard each action: one failing notification must not
        // abort the loop and silently skip the remaining stalled builds in this pass.
        for (buildId in stalledBuilds) {
            LOG.info("Build $buildId has stalled")
            runningBuilds.remove(buildId) // Prevent multiple notifications
            try {
                action(buildId)
            } catch (e: Exception) {
                LOG.warn("Failed to process stalled build $buildId", e)
            }
        }
    }
}