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
import net.gini.android.core.api.response.ErrorResponse
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import net.gini.android.internal.payment.GiniHealthException
import org.slf4j.LoggerFactory
import java.util.Date
import kotlin.coroutines.CoroutineContext

class InvoicesRepository(
    private val giniHealthAPI: GiniHealthAPI,
    val giniHealth: GiniHealth,
    private val hardcodedInvoicesLocalDataSource: HardcodedInvoicesLocalDataSource,
    private val invoicesLocalDataSource: InvoicesLocalDataSource,
    val coroutineContext: CoroutineContext = Dispatchers.IO
) {

    private val _uploadHardcodedInvoicesStateFlow: MutableStateFlow<UploadHardcodedInvoicesState> =
        MutableStateFlow(
            UploadHardcodedInvoicesState.Idle
        )
    val uploadHardcodedInvoicesStateFlow = _uploadHardcodedInvoicesStateFlow.asStateFlow()

    private val _extractionErrorFlow = MutableStateFlow<Exception?>(null)
    val extractionErrorFlow = _extractionErrorFlow.asStateFlow()

    val invoicesFlow = invoicesLocalDataSource.invoicesFlow

    suspend fun loadInvoicesWithExtractions() {
        invoicesLocalDataSource.loadInvoicesWithExtractions()
    }

    suspend fun uploadHardcodedInvoices() = withContext(coroutineContext) {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading

        val hardcodedInvoices = hardcodedInvoicesLocalDataSource.getHardcodedInvoices()

        // Each async returns Pair<DocumentWithExtractions?, Resource<*>> — no shared mutable state
        val results = hardcodedInvoices.map { invoiceBytes ->
            async {
                var extractedDocument: DocumentWithExtractions? = null
                val resource = giniHealthAPI.documentManager.createPartialDocument(
                    invoiceBytes,
                    MediaTypes.IMAGE_JPEG
                ).mapSuccess { partialDocumentResource ->
                    giniHealthAPI.documentManager.createCompositeDocument(
                        listOf(
                            partialDocumentResource.data
                        )
                    )
                }.mapSuccess { compositeDocumentResource ->
                    val documentWithExtractions =
                        getDocumentWithExtraction(compositeDocumentResource.data)
                    extractedDocument = documentWithExtractions.first
                    documentWithExtractions.second
                }
                Pair(extractedDocument, resource)
            }
        }.awaitAll()

        val documentsWithExtractions = results.mapNotNull { it.first }

        val errors = results.mapNotNull { (_, resource) ->
            if (resource is Resource.Error) {
                // Error is already parsed by Resource!
                val errorMessage = resource.errorResponse?.message
                    ?: resource.exception?.message
                    ?: resource.message
                    ?: "Unknown error"

                ErrorDetail(
                    message = errorMessage,
                    statusCode = resource.responseStatusCode,
                    errorResponse = resource.errorResponse  // Already parsed!
                )
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
    }

    suspend fun refreshInvoices() = withContext(coroutineContext) {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Loading

        // Each async returns DocumentWithExtractions? — results collected after awaitAll, no shared mutable state
        val jobs = invoicesFlow.value.map { document ->
            async {
                val emptyDocument = createEmptyDocument(document.documentId)
                when (val allExtraction =
                    giniHealthAPI.documentManager.getAllExtractions(createEmptyDocument(documentId = document.documentId))) {
                    is Resource.Success -> {
                        try {
                            val isPayable = giniHealth.checkIfDocumentIsPayable(emptyDocument.id)
                            DocumentWithExtractions.fromDocumentAndExtractions(
                                emptyDocument,
                                allExtraction.data,
                                isPayable
                            )
                        } catch (e: Exception) {
                            _extractionErrorFlow.value = e
                            DocumentWithExtractions.fromDocumentAndExtractions(
                                emptyDocument,
                                allExtraction.data,
                                false
                            )
                        }
                    }

                    is Resource.Error -> {
                        // Emit error when extraction API fails
                        val exception = GiniHealthException(
                            message = allExtraction.exception?.message ?: allExtraction.message
                            ?: "Failed to get extractions",
                            cause = allExtraction.exception,
                            statusCode = allExtraction.responseStatusCode,
                            errorResponse = allExtraction.errorResponse
                        )
                        _extractionErrorFlow.value = exception
                        null
                    }

                    else -> null
                }
            }
        }

        val documentsWithExtractions = jobs.awaitAll().filterNotNull()
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
        documentId,
        Document.ProcessingState.COMPLETED,
        "",
        0,
        Date(),
        null,
        Document.SourceClassification.UNKNOWN,
        Uri.EMPTY,
        emptyList(),
        emptyList()
    )

    private suspend fun getDocumentWithExtraction(document: Document): Pair<DocumentWithExtractions?, Resource<ExtractionsContainer>> {
        return when (val extractionsResource =
            giniHealthAPI.documentManager.getAllExtractionsWithPolling(document)) {
            is Resource.Success -> {
                try {
                    val isPayable = giniHealth.checkIfDocumentIsPayable(document.id)
                    val documentWithExtractions =
                        DocumentWithExtractions.fromDocumentAndExtractions(
                            document,
                            extractionsResource.data,
                            isPayable
                        )
                    Pair(documentWithExtractions, extractionsResource)
                } catch (e: Exception) {

                    _extractionErrorFlow.value = e
                    LOG.error(
                        "Error checking if document ${document.id} is payable: ${e.message}",
                        e
                    )

                    val documentWithExtractions =
                        DocumentWithExtractions.fromDocumentAndExtractions(
                            document,
                            extractionsResource.data,
                            false
                        )
                    Pair(documentWithExtractions, extractionsResource)
                }
            }

            is Resource.Error -> {
                // Emit error when extraction API fails
                val exception = GiniHealthException(
                    message = extractionsResource.exception?.message
                        ?: extractionsResource.message ?: "Failed to get extractions",
                    cause = extractionsResource.exception,
                    statusCode = extractionsResource.responseStatusCode,
                    errorResponse = extractionsResource.errorResponse
                )
                _extractionErrorFlow.value = exception

                Pair(null, extractionsResource)
            }

            else -> Pair(null, extractionsResource)
        }
    }

    suspend fun deleteDocuments(documentIds: List<String>) {
        invoicesLocalDataSource.deleteDocuments(documentIds)
    }

    fun resetUploadState() {
        _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Idle
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesRepository::class.java)
    }
}

sealed class UploadHardcodedInvoicesState {
    object Idle : UploadHardcodedInvoicesState()
    object Loading : UploadHardcodedInvoicesState()
    object Success : UploadHardcodedInvoicesState()
    data class Failure(
        val errors: List<ErrorDetail>
    ) : UploadHardcodedInvoicesState()
}

/**
 * Represents error details for upload operations.
 * Now uses parsed ErrorResponse instead of raw JSON.
 */
data class ErrorDetail(
    val message: String,
    val statusCode: Int? = null,
    val errorResponse: ErrorResponse? = null,
)

