package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class InvoiceEducationStorage(
    val context: Context
) {

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

    private val invoiceRecognitionCount = intPreferencesKey("invoice_recognition_count")

    fun getInvoiceRecognitionCount(): Flow<Int> {
        return context.dataStore.data.map {
            it[invoiceRecognitionCount] ?: 0
        }
    }

    suspend fun setInvoiceRecognitionCount(count: Int) {
        context.dataStore.updateData {
            it.toMutablePreferences().apply {
                set(invoiceRecognitionCount, count)
            }
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "invoice_education_storage"
    }

}
