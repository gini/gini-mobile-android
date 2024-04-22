package net.gini.android.health.sdk.review

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "review-preferences")

internal class ReviewPreferences(private val context: Context) {
    suspend fun saveCountForPaymentProviderId(paymentProviderId: String, count: Int) {
        context.dataStore.edit { preferences ->
            val key = intPreferencesKey(paymentProviderId)
            preferences[key] = count
        }
    }

    suspend fun getCountForPaymentProviderId(paymentProviderId: String): Int? {
        return context.dataStore.data.map { preferences ->
            val key = intPreferencesKey(paymentProviderId)
            preferences[key]
        }.first()
    }
}