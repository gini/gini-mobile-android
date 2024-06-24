package net.gini.android.capture.internal.provider

import android.content.Context
import java.util.UUID

internal class InstallationIdProvider(context: Context) {

    companion object {
        private const val SP_NAME = "InstallationIdPref"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    private val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun getInstallationId(): String = sp.getString(
        KEY_INSTALLATION_ID, null
    ) ?: UUID.randomUUID().toString().also { setInstallationId(it) }


    private fun setInstallationId(installationId: String) {
        sp.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
    }
}