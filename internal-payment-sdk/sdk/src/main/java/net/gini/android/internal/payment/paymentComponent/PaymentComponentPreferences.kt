package net.gini.android.internal.payment.paymentComponent

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "payment-component-preferences")

private val KEY_SELECTED_PAYMENT_PROVIDER_ID = stringPreferencesKey("selected-payment-provider-id")
private val KEY_RETURNING_USER = booleanPreferencesKey("returning-user")

class PaymentComponentPreferences(private val context: Context) {

    suspend fun saveSelectedPaymentProviderId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_PAYMENT_PROVIDER_ID] = id
        }
    }

    suspend fun deleteSelectedPaymentProviderId() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_SELECTED_PAYMENT_PROVIDER_ID)
        }
    }

    suspend fun getSelectedPaymentProviderId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SELECTED_PAYMENT_PROVIDER_ID]
        }.first()
    }

    suspend fun saveReturningUser() {
        context.dataStore.edit { preferences ->
            preferences[KEY_RETURNING_USER] = true
        }
    }

    suspend fun getReturningUser(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_RETURNING_USER]
        }.firstOrNull() ?: false
    }

    suspend fun clearData() {
        context.dataStore.edit { it.clear() }
    }
}