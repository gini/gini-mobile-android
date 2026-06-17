package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ClientConfigurationStorage(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

    private val keyUnsupportedQrCodeWarningEnabled =
        booleanPreferencesKey("is_unsupported_qr_code_warning_enabled")

    fun getIsUnsupportedQRCodeWarningEnabled(): Flow<Boolean> =
        context.dataStore.data.map { it[keyUnsupportedQrCodeWarningEnabled] ?: false }

    suspend fun setIsUnsupportedQRCodeWarningEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyUnsupportedQrCodeWarningEnabled] = enabled
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "client_configuration_storage"
    }
}
