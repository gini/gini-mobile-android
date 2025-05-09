package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class QrCodeEducationStorage(
    val context: Context
) {

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

    private val keyQrCodeRecognitionCount = intPreferencesKey("qr_code_recognition_count")

    fun getQrCodeRecognitionCount(): Flow<Int> {
        return context.dataStore.data.map {
            it[keyQrCodeRecognitionCount] ?: 0
        }
    }

    suspend fun setQrCodeRecognitionCount(count: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().apply {
                set(keyQrCodeRecognitionCount, count)
            }
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "qr_code_education_storage"
    }

}
