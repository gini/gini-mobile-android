package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "payment-component-preferences")

private val KEY_SELECTED_PAYMENT_PROVIDER_ID = stringPreferencesKey("selected-payment-provider-id")

internal class PaymentComponentPreferences(private val context: Context) {

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

}
