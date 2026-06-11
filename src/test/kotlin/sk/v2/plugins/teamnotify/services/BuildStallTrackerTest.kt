package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SRunningBuild
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class BuildStallTrackerTest {

    private lateinit var buildStallTracker: BuildStallTracker
    // Controlled clock so stall detection is deterministic and independent of wall-clock timing
    private var currentTime = 1_000L

    @BeforeEach
    fun setUp() {
        currentTime = 1_000L
        buildStallTracker = BuildStallTracker()
        buildStallTracker.clock = { currentTime }
    }

    @Test
    fun `startTracking adds build to tracking list`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)
        currentTime += 500 // build has now been inactive for 500 ms

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
        currentTime += 500

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
        currentTime += 500 // exceeds the 0 ms timeout

        var triggered = false
        buildStallTracker.checkForStalledBuilds(0) { _ ->
            triggered = true
        }
        assert(triggered)
    }

    @Test
    fun `checkForStalledBuilds does not trigger for non-stalled builds`() {
        val build = mock(SRunningBuild::class.java)
        `when`(build.buildId).thenReturn(1L)

        buildStallTracker.startTracking(build)
        currentTime += 500

        var triggered = false
        buildStallTracker.checkForStalledBuilds(Long.MAX_VALUE) { _ ->
            triggered = true
        }
        assert(!triggered)
    }
}