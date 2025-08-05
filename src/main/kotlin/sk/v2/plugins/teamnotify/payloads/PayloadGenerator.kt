package sk.v2.plugins.teamnotify.payloads

import jetbrains.buildServer.serverSide.SRunningBuild

interface PayloadGenerator {
    fun generatePayload(build: SRunningBuild, message: String): String
}