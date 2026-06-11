package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer

class BuildDurationService(private val sBuildServer: SBuildServer) {

    private companion object {
        // Cap how much history is averaged so the whole build history isn't materialized on a hot path
        const val MAX_BUILDS = 50
    }

    fun getAverageBuildDuration(buildTypeId: String): Long {
        val buildType = sBuildServer.projectManager.findBuildTypeById(buildTypeId) ?: return 0
        val recent = buildType.getHistory(null, true, true).take(MAX_BUILDS)
        if (recent.isEmpty()) return 0
        return recent.map { it.duration }.average().toLong()
    }
}