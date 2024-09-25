package net.gini.android.bank.sdk.transactiondocs.internal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.gini.android.bank.sdk.transactiondocs.TransactionDocsSettings

internal class GiniTransactionDocsSettings(
    private val context: Context,
) : TransactionDocsSettings {

    private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

    override fun getAlwaysAttachSetting(): Flow<Boolean> {
        return context.dataStore.data.map {
            it[booleanPreferencesKey(PARAM_NAME_ALWAYS_ATTACH)] ?: DEFAULT_ALWAYS_ATTACH
        }
    }

    override suspend fun setAlwaysAttachSetting(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[booleanPreferencesKey(PARAM_NAME_ALWAYS_ATTACH)] = value
        }
    }

    companion object {
        private const val DATA_STORE_NAME = "gini_transaction_docs_settings"

        private const val PARAM_NAME_ALWAYS_ATTACH = "always_attach"
        private const val DEFAULT_ALWAYS_ATTACH = false
    }
}
