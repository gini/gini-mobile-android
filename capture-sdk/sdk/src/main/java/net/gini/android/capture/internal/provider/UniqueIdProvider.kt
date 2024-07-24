package net.gini.android.capture.internal.provider

import android.content.Context
import java.util.UUID

internal class UniqueIdProvider(context: Context) {

    companion object {
        private const val SP_NAME = "InstallationIdPref"
    }

    private val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun getUniqueId(key: String): String = sp.getString(
        key, null
    ) ?: UUID.randomUUID().toString().also { setUniqueId(it, key) }


    private fun setUniqueId(installationId: String, key: String) {
        sp.edit().putString(key, installationId).apply()
    }
}