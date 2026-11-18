package sk.v2.plugins.teamnotify.utils

import jetbrains.buildServer.serverSide.SProject

/**
 * Versioned-settings state of a project. Read reflectively because the methods are not part of the
 * stable open API; any failure counts as "not versioned", which keeps the UI editable.
 */
object VersionedSettings {

    fun isEnabled(project: SProject): Boolean = try {
        project.javaClass.getMethod("isVersionedSettingsEnabled").invoke(project) as Boolean
    } catch (e: Exception) {
        false
    }

    /** True when settings are versioned and TeamCity does not allow editing them in the UI. */
    fun isReadOnly(project: SProject): Boolean = try {
        isEnabled(project) && !(project.javaClass.getMethod("isVersionedSettingsAllowUIEditing").invoke(project) as Boolean)
    } catch (e: Exception) {
        false
    }
}
