package net.gini.android.merchant.sdk.exampleapp.invoices.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.core.api.MediaTypes
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import java.util.Date

class InvoicesRepository(
    private val giniHealthAPI: GiniHealthAPI,
    val giniMerchant: GiniMerchant,
    private val hardcodedInvoicesLocalDataSource: HardcodedInvoicesLocalDataSource,
    private val invoicesLocalDataSource: InvoicesLocalDataSource
) {

    private val _uploadHardcodedInvoicesStateFlow: MutableStateFlow<UploadHardcodedInvoicesState> = MutableStateFlow(
        UploadHardcodedInvoicesState.Idle
    )
    val uploadHardcodedInvoicesStateFlow = _uploadHardcodedInvoicesStateFlow.asStateFlow()

    val invoicesFlow = invoicesLocalDataSource.invoicesFlow

    suspend fun loadInvoicesWithExtractions() {
        invoicesLocalDataSource.loadInvoicesWithExtractions()
    }

    suspend fun uploadHardcodedInvoices() {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading

        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()

        val hardcodedInvoices = hardcodedInvoicesLocalDataSource.getHardcodedInvoices()
        val createdResources = hardcodedInvoices.map { invoiceBytes ->
            giniHealthAPI.documentManager.createPartialDocument(
                invoiceBytes,
                MediaTypes.IMAGE_JPEG
            ).mapSuccess { partialDocumentResource ->
                giniHealthAPI.documentManager.createCompositeDocument(listOf(partialDocumentResource.data))
            }.mapSuccess { compositeDocumentResource ->
                val documentWithExtractions = getDocumentWithExtraction(compositeDocumentResource.data)
                documentWithExtractions.first?.let { doc ->
                    documentsWithExtractions.add(doc)
                }
                documentWithExtractions.second
            }
        }

        invoicesLocalDataSource.appendInvoicesWithExtractions(documentsWithExtractions)

        val errors = createdResources.mapNotNull { resource ->
            if (resource is Resource.Error) {
                resource.message
            } else {
                null
            }
        }

        if (errors.isEmpty()) {
            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Success
        } else {
            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Failure(errors)
        }

        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Idle
    }

    suspend fun refreshInvoices() {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading
        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()
        invoicesFlow.value.forEach { document ->
            val emptyDocument = createEmptyDocument(document.documentId)
            giniHealthAPI.documentManager.getAllExtractions(createEmptyDocument(documentId = document.documentId)).mapSuccess {
                val isPayable = giniMerchant.checkIfDocumentIsPayable(emptyDocument.id)
                val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                    emptyDocument,
                    it.data,
                    isPayable
                )
                documentsWithExtractions.add(documentWithExtractions)
                it
            }
        }
        invoicesLocalDataSource.refreshInvoices(documentsWithExtractions)
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Success
    }

    suspend fun requestDocumentExtractionAndSaveToLocal(document: Document) {
        val documentWithExtractions = getDocumentWithExtraction(document)
        documentWithExtractions.first?.let { doc ->
            invoicesLocalDataSource.updateInvoice(doc)
        }
    }

    private fun createEmptyDocument(documentId: String) = Document(
        documentId, Document.ProcessingState.COMPLETED, "", 0, Date(), Document.SourceClassification.UNKNOWN, Uri.EMPTY, emptyList(), emptyList()
    )

    private suspend fun getDocumentWithExtraction(document: Document): Pair<DocumentWithExtractions?, Resource<ExtractionsContainer>> {
        return when (val extractionsResource = giniHealthAPI.documentManager.getAllExtractionsWithPolling(document)) {
            is Resource.Success -> {
                val isPayable = giniMerchant.checkIfDocumentIsPayable(document.id)
                val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                    document,
                    extractionsResource.data,
                    isPayable
                )
                Pair(documentWithExtractions, extractionsResource)
            }
            else -> Pair(null, extractionsResource)
        }
    }
}

sealed class UploadHardcodedInvoicesState {
    object Idle : UploadHardcodedInvoicesState()
    object Loading : UploadHardcodedInvoicesState()
    object Success : UploadHardcodedInvoicesState()
    data class Failure(val errors: List<String>) : UploadHardcodedInvoicesState()
}
