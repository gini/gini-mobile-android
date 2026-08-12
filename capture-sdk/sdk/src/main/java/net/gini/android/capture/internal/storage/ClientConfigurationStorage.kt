package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.gini.android.capture.internal.network.Configuration

// Declared at the top level (per AndroidX DataStore guidance) so there is exactly one
// DataStore per file in the process, regardless of how many ClientConfigurationStorage
// instances are created. Declaring it as an instance member would open a new DataStore
// for the same file per instance and throw "There are multiple DataStores active for the
// same file".
private val Context.dataStore by preferencesDataStore(name = ClientConfigurationStorage.DATA_STORE_NAME)

internal class ClientConfigurationStorage(private val context: Context) {

    private val keyIsCached = booleanPreferencesKey("is_cached")
    private val keyIsUserJourneyAnalyticsEnabled =
        booleanPreferencesKey("is_user_journey_analytics_enabled")
    private val keyIsSkontoEnabled = booleanPreferencesKey("is_skonto_enabled")
    private val keyIsReturnAssistantEnabled = booleanPreferencesKey("is_return_assistant_enabled")
    private val keyIsTransactionDocsEnabled = booleanPreferencesKey("is_transaction_docs_enabled")
    private val keyIsQrCodeEducationEnabled = booleanPreferencesKey("is_qr_code_education_enabled")
    private val keyIsInstantPaymentEnabled = booleanPreferencesKey("is_instant_payment_enabled")
    private val keyIsEInvoiceEnabled = booleanPreferencesKey("is_e_invoice_enabled")
    private val keyIsSavePhotosLocallyEnabled =
        booleanPreferencesKey("is_save_photos_locally_enabled")
    private val keyIsAlreadyPaidHintEnabled = booleanPreferencesKey("is_already_paid_hint_enabled")
    private val keyIsPaymentDueHintEnabled = booleanPreferencesKey("is_payment_due_hint_enabled")
    private val keyIsUnsupportedQRCodeWarningEnabled =
        booleanPreferencesKey("is_unsupported_qr_code_warning_enabled")

    fun getConfiguration(): Flow<Configuration?> = context.dataStore.data.map { prefs ->
        if (prefs[keyIsCached] != true) return@map null
        Configuration(
            clientID = "",
            amplitudeApiKey = "",
            isUserJourneyAnalyticsEnabled = prefs[keyIsUserJourneyAnalyticsEnabled] ?: false,
            isSkontoEnabled = prefs[keyIsSkontoEnabled] ?: false,
            isReturnAssistantEnabled = prefs[keyIsReturnAssistantEnabled] ?: false,
            isTransactionDocsEnabled = prefs[keyIsTransactionDocsEnabled] ?: false,
            isQrCodeEducationEnabled = prefs[keyIsQrCodeEducationEnabled] ?: false,
            isInstantPaymentEnabled = prefs[keyIsInstantPaymentEnabled] ?: false,
            isEInvoiceEnabled = prefs[keyIsEInvoiceEnabled] ?: false,
            isSavePhotosLocallyEnabled = prefs[keyIsSavePhotosLocallyEnabled] ?: false,
            isAlreadyPaidHintEnabled = prefs[keyIsAlreadyPaidHintEnabled] ?: false,
            isPaymentDueHintEnabled = prefs[keyIsPaymentDueHintEnabled] ?: false,
            isUnsupportedQRCodeWarningEnabled = prefs[keyIsUnsupportedQRCodeWarningEnabled]
                ?: false,
        )
    }

    suspend fun clearConfiguration() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun saveConfiguration(configuration: Configuration) {
        context.dataStore.edit { prefs ->
            prefs[keyIsUserJourneyAnalyticsEnabled] = configuration.isUserJourneyAnalyticsEnabled
            prefs[keyIsSkontoEnabled] = configuration.isSkontoEnabled
            prefs[keyIsReturnAssistantEnabled] = configuration.isReturnAssistantEnabled
            prefs[keyIsTransactionDocsEnabled] = configuration.isTransactionDocsEnabled
            prefs[keyIsQrCodeEducationEnabled] = configuration.isQrCodeEducationEnabled
            prefs[keyIsInstantPaymentEnabled] = configuration.isInstantPaymentEnabled
            prefs[keyIsEInvoiceEnabled] = configuration.isEInvoiceEnabled
            prefs[keyIsSavePhotosLocallyEnabled] = configuration.isSavePhotosLocallyEnabled
            prefs[keyIsAlreadyPaidHintEnabled] = configuration.isAlreadyPaidHintEnabled
            prefs[keyIsPaymentDueHintEnabled] = configuration.isPaymentDueHintEnabled
            prefs[keyIsUnsupportedQRCodeWarningEnabled] =
                configuration.isUnsupportedQRCodeWarningEnabled
            prefs[keyIsCached] = true
        }
    }

    companion object {
        const val DATA_STORE_NAME = "client_configuration_storage"
    }
}
