package net.gini.android.health.sdk.exampleapp.invoices.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions

private val Context.dataStore by preferencesDataStore(name = "invoices")
private val KEY_INVOICES = stringPreferencesKey("invoices")

class InvoicesLocalDataSource(private val context: Context) {

    private val _invoicesFlow: MutableStateFlow<List<DocumentWithExtractions>> = MutableStateFlow(listOf())
    val invoicesFlow = _invoicesFlow.asStateFlow()

    private val moshi: Moshi = Moshi.Builder().build()

    @OptIn(ExperimentalStdlibApi::class)
    private val jsonAdapter: JsonAdapter<List<DocumentWithExtractions>> = moshi.adapter<List<DocumentWithExtractions>>()

    suspend fun loadInvoicesWithExtractions() {
        _invoicesFlow.value = readInvoicesFromPreferences()
    }

    suspend fun appendInvoicesWithExtractions(newInvoices: List<DocumentWithExtractions>) {
        val storedInvoices = readInvoicesFromPreferences().toMutableList()
        storedInvoices.addAll(newInvoices)
        writeInvoicesToPreferences(storedInvoices)
        _invoicesFlow.value = storedInvoices
    }

    suspend fun updateInvoice(documentWithExtractions: DocumentWithExtractions) {
        val documentsList = readInvoicesFromPreferences()
        val updatedDocument = documentsList.firstOrNull { it.documentId == documentWithExtractions.documentId }
        updatedDocument ?: return
        if (updatedDocument.shouldUpdate(documentWithExtractions.amount, documentWithExtractions.recipient)) {
            updatedDocument.amount = documentWithExtractions.amount
            updatedDocument.recipient = documentWithExtractions.recipient
            writeInvoicesToPreferences(documentsList)
            _invoicesFlow.value = documentsList
        }
    }

    suspend fun refreshInvoices(documentsList: List<DocumentWithExtractions>) {
        val storedDocumentsList = readInvoicesFromPreferences()
        documentsList.forEach { newDocument ->
            val document = storedDocumentsList.firstOrNull { it.documentId == newDocument.documentId }
            document?.let {
                it.amount = newDocument.amount
                it.recipient = newDocument.recipient
            }
        }
        writeInvoicesToPreferences(storedDocumentsList)
        _invoicesFlow.value = storedDocumentsList
    }

    suspend fun appendInvoiceWithExtractions(invoice: DocumentWithExtractions) {
        val storedInvoices = readInvoicesFromPreferences().toMutableList()
        storedInvoices.add(invoice)
        writeInvoicesToPreferences(storedInvoices)
        _invoicesFlow.value = storedInvoices
    }

    private suspend fun readInvoicesFromPreferences(): List<DocumentWithExtractions> {
        return context.dataStore.data.map { preferences ->
            val invoicesJson = preferences[KEY_INVOICES] ?: ""
            if (invoicesJson.isNotEmpty()) {
                jsonAdapter.fromJson(invoicesJson) ?: emptyList()
            } else {
                emptyList()
            }
        }.first()
    }

    private suspend fun writeInvoicesToPreferences(invoices: List<DocumentWithExtractions>) {
        val invoicesJson = jsonAdapter.toJson(invoices)
        context.dataStore.edit { preferences ->
            preferences[KEY_INVOICES] = invoicesJson
        }
    }
}