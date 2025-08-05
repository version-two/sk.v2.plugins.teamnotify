package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.serverSide.SFinishedBuild
import jetbrains.buildServer.serverSide.SProjectManager
import jetbrains.buildServer.serverSide.SBuildType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class BuildDurationServiceTest {

    private lateinit var sBuildServer: SBuildServer
    private lateinit var projectManager: SProjectManager
    private lateinit var buildType: SBuildType
    private lateinit var buildDurationService: BuildDurationService

    @BeforeEach
    fun setUp() {
        sBuildServer = mock(SBuildServer::class.java)
        projectManager = mock(SProjectManager::class.java)
        buildType = mock(SBuildType::class.java)

        `when`(sBuildServer.projectManager).thenReturn(projectManager)
        `when`(projectManager.findBuildTypeById(anyString())).thenReturn(buildType)

        buildDurationService = BuildDurationService(sBuildServer)
    }

    @Test
    fun `getAverageBuildDuration returns 0 if no builds in history`() {
        `when`(buildType.getHistory(anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(emptyList())

        val averageDuration = buildDurationService.getAverageBuildDuration("testBuildTypeId")

        assert(averageDuration == 0L)
    }

    @Test
    fun `getAverageBuildDuration returns correct average duration`() {
        val build1 = mock(SFinishedBuild::class.java)
        `when`(build1.duration).thenReturn(100L)
        val build2 = mock(SFinishedBuild::class.java)
        `when`(build2.duration).thenReturn(200L)
        val build3 = mock(SFinishedBuild::class.java)
        `when`(build3.duration).thenReturn(300L)

        `when`(buildType.getHistory(anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(listOf(build1, build2, build3))

        val averageDuration = buildDurationService.getAverageBuildDuration("testBuildTypeId")

        assert(averageDuration == 200L)
    }
}