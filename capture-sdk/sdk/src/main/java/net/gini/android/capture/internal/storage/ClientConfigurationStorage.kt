package net.gini.android.capture.internal.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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
    private val keyIsPaymentScheduleHintEnabled =
        booleanPreferencesKey("is_payment_schedule_hint_enabled")
    private val keyIsCreditNoteHintEnabled = booleanPreferencesKey("is_credit_note_hint_enabled")

    fun getConfiguration(): Flow<Configuration?> = context.dataStore.data.map { prefs ->
        if (prefs[keyIsCached] != true) return@map null
        Configuration(
            clientID = "",
            amplitudeApiKey = "",
            isUserJourneyAnalyticsEnabled = prefs.flag(keyIsUserJourneyAnalyticsEnabled),
            isSkontoEnabled = prefs.flag(keyIsSkontoEnabled),
            isReturnAssistantEnabled = prefs.flag(keyIsReturnAssistantEnabled),
            isTransactionDocsEnabled = prefs.flag(keyIsTransactionDocsEnabled),
            isQrCodeEducationEnabled = prefs.flag(keyIsQrCodeEducationEnabled),
            isInstantPaymentEnabled = prefs.flag(keyIsInstantPaymentEnabled),
            isEInvoiceEnabled = prefs.flag(keyIsEInvoiceEnabled),
            isSavePhotosLocallyEnabled = prefs.flag(keyIsSavePhotosLocallyEnabled),
            isAlreadyPaidHintEnabled = prefs.flag(keyIsAlreadyPaidHintEnabled),
            isPaymentDueHintEnabled = prefs.flag(keyIsPaymentDueHintEnabled),
            isUnsupportedQRCodeWarningEnabled = prefs.flag(keyIsUnsupportedQRCodeWarningEnabled),
            isPaymentScheduleHintEnabled = prefs.flag(keyIsPaymentScheduleHintEnabled),
            isCreditNoteHintEnabled = prefs.flag(keyIsCreditNoteHintEnabled),
        )
    }

    private fun Preferences.flag(key: Preferences.Key<Boolean>): Boolean = this[key] ?: false

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
            prefs[keyIsPaymentScheduleHintEnabled] = configuration.isPaymentScheduleHintEnabled
            prefs[keyIsCreditNoteHintEnabled] =
                configuration.isCreditNoteHintEnabled
            prefs[keyIsCached] = true
        }
    }

    companion object {
        const val DATA_STORE_NAME = "client_configuration_storage"
    }
}
