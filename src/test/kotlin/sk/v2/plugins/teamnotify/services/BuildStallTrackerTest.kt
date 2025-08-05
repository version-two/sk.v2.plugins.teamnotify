package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SRunningBuild
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class BuildStallTrackerTest {

    private lateinit var buildStallTracker: BuildStallTracker

    @BeforeEach
    fun setUp() {
        buildStallTracker = BuildStallTracker()
    }

    @Test
    fun `startTracking adds build to tracking list`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)

        // No direct way to assert internal state, but we can check behavior with checkForStalledBuilds
        var stalledBuildId: Long? = null
        buildStallTracker.checkForStalledBuilds(0) { buildId ->
            stalledBuildId = buildId
        }
        assert(stalledBuildId == 1L)
    }

    @Test
    fun `stopTracking removes build from tracking list`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)
        buildStallTracker.stopTracking(build)

        var stalledBuildId: Long? = null
        buildStallTracker.checkForStalledBuilds(0) { buildId ->
            stalledBuildId = buildId
        }
        assert(stalledBuildId == null)
    }

    @Test
    fun `checkForStalledBuilds triggers action for stalled builds`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)

        var triggered = false
        buildStallTracker.checkForStalledBuilds(0) { buildId ->
            triggered = true
        }
        assert(triggered)
    }

    @Test
    fun `checkForStalledBuilds does not trigger for non-stalled builds`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)

        var triggered = false
        buildStallTracker.checkForStalledBuilds(Long.MAX_VALUE) { buildId ->
            triggered = true
        }
        assert(!triggered)
    }
}