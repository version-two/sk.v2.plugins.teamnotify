package sk.v2.plugins.teamnotify.services

import jetbrains.buildServer.serverSide.SBuildServer

class BuildDurationService(private val sBuildServer: SBuildServer) {

    fun getAverageBuildDuration(buildTypeId: String): Long {
        val buildType = sBuildServer.projectManager.findBuildTypeById(buildTypeId)
        if (buildType != null) {
            val history = buildType.getHistory(null, true, true)
            if (history.isNotEmpty()) {
                return history.map { it.duration }.average().toLong()
            }
        }
        return 0
    }
}