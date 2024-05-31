package net.gini.android.merchant.sdk.review.openWith

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "open-with-preferences")
internal class OpenWithPreferences(private val context: Context) {
    suspend fun incrementCountForPaymentProviderId(paymentProviderId: String) {
        context.dataStore.edit { preferences ->
            val key = intPreferencesKey(paymentProviderId)
            val value = preferences[key]
            preferences[key] = if (value == null) 1 else value + 1
        }
    }

    fun getLiveCountForPaymentProviderId(paymentProviderId: String): Flow<Int?> = context.dataStore.data.map { preferences -> preferences[intPreferencesKey(paymentProviderId)] }.distinctUntilChanged()
}