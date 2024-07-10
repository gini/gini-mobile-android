package net.gini.android.capture.internal.provider

import android.content.Context
import java.util.UUID

internal class UserIdProvider(context: Context) {

    companion object {
        private const val SP_NAME = "UserIdPref"
        private const val KEY_USER_ID = "user_id"
    }

    private val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun getUserId(): String = sp.getString(
        KEY_USER_ID, null
    ) ?: UUID.randomUUID().toString().also { setUserId(it) }


    private fun setUserId(installationId: String) {
        sp.edit().putString(KEY_USER_ID, installationId).apply()
    }
}