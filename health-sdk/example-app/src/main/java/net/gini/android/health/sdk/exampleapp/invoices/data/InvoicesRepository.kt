package net.gini.android.health.sdk.exampleapp.invoices.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.gini.android.core.api.MediaTypes
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import java.util.Date
import kotlin.coroutines.CoroutineContext

class InvoicesRepository(
    private val giniHealthAPI: GiniHealthAPI,
    val giniHealth: GiniHealth,
    private val hardcodedInvoicesLocalDataSource: HardcodedInvoicesLocalDataSource,
    private val invoicesLocalDataSource: InvoicesLocalDataSource,
    val coroutineContext: CoroutineContext = Dispatchers.IO
) {

    private val _uploadHardcodedInvoicesStateFlow: MutableStateFlow<UploadHardcodedInvoicesState> = MutableStateFlow(
        UploadHardcodedInvoicesState.Idle
    )
    val uploadHardcodedInvoicesStateFlow = _uploadHardcodedInvoicesStateFlow.asStateFlow()

    val invoicesFlow = invoicesLocalDataSource.invoicesFlow

    suspend fun loadInvoicesWithExtractions() {
        invoicesLocalDataSource.loadInvoicesWithExtractions()
    }

    suspend fun uploadHardcodedInvoices() = withContext(coroutineContext) {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading

        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()

        val hardcodedInvoices = hardcodedInvoicesLocalDataSource.getHardcodedInvoices()
        val createdResources = hardcodedInvoices.map { invoiceBytes ->
            async {
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
        }

        val errors = createdResources.awaitAll().mapNotNull { resource ->
            if (resource is Resource.Error) {
                resource.message
            } else {
                null
            }
        }

        if (errors.isEmpty()) {
            invoicesLocalDataSource.appendInvoicesWithExtractions(documentsWithExtractions)

            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Success
        } else {
            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Failure(errors)
        }

        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Idle
    }

    suspend fun refreshInvoices() = withContext(coroutineContext) {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading
        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()
        val jobs = invoicesFlow.value.map { document ->
            async {
                val emptyDocument = createEmptyDocument(document.documentId)
                giniHealthAPI.documentManager.getAllExtractions(createEmptyDocument(documentId = document.documentId))
                    .mapSuccess {
                        val isPayable = giniHealth.checkIfDocumentIsPayable(emptyDocument.id)
                        val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                            emptyDocument,
                            it.data,
                            isPayable
                        )
                        documentsWithExtractions.add(documentWithExtractions)
                        it
                    }
            }
        }
        jobs.awaitAll()
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
        documentId, Document.ProcessingState.COMPLETED, "", 0, Date(), null, Document.SourceClassification.UNKNOWN, Uri.EMPTY, emptyList(), emptyList()
    )

    private suspend fun getDocumentWithExtraction(document: Document): Pair<DocumentWithExtractions?, Resource<ExtractionsContainer>> {
        return when (val extractionsResource = giniHealthAPI.documentManager.getAllExtractionsWithPolling(document)) {
            is Resource.Success -> {
                val isPayable = giniHealth.checkIfDocumentIsPayable(document.id)
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
