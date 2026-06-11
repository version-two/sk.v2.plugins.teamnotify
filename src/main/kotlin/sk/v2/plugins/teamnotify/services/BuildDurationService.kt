package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer

class BuildDurationService(private val sBuildServer: SBuildServer) {

    private companion object {
        // Cap how much history is averaged so the whole build history isn't materialized on a hot path
        const val MAX_BUILDS = 50
    }

    fun getAverageBuildDuration(buildTypeId: String): Long {
        val buildType = sBuildServer.projectManager.findBuildTypeById(buildTypeId) ?: return 0
        // getHistory(user, includePersonal, includeCanceled): exclude personal and canceled builds so
        // the average reflects real, completed builds rather than aborted or developer scratch runs.
        val recent = buildType.getHistory(null, false, false).take(MAX_BUILDS)
        if (recent.isEmpty()) return 0
        return recent.map { it.duration }.average().toLong()
    }
}