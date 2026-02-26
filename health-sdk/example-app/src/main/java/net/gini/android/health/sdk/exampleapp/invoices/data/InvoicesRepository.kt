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
                // Error is already parsed by Resource!
                val errorMessage = resource.errorResponse?.items?.firstOrNull()?.message
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
        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()
        val errors = mutableListOf<ErrorDetail>()

        val jobs = invoicesFlow.value.map { document ->
            async {
                val emptyDocument = createEmptyDocument(document.documentId)
                val extractionsResult = giniHealthAPI.documentManager.getAllExtractions(
                    createEmptyDocument(documentId = document.documentId)
                )

                when (extractionsResult) {
                    is Resource.Success -> {
                        try {
                            val isPayable = giniHealth.checkIfDocumentIsPayable(emptyDocument.id)
                            val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                                emptyDocument,
                                extractionsResult.data,
                                isPayable
                            )
                            documentsWithExtractions.add(documentWithExtractions)
                        } catch (e: GiniHealthException) {
                            // Store document with unknown payable status (default to false)
                            val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                                emptyDocument,
                                extractionsResult.data,
                                false
                            )
                            documentsWithExtractions.add(documentWithExtractions)
                        }
                    }
                    is Resource.Error -> {
                        // Error is already parsed by Resource!
                        val errorMessage = extractionsResult.errorResponse?.items?.firstOrNull()?.message
                            ?: extractionsResult.exception?.message
                            ?: "Failed to get extractions for document ${emptyDocument.id}"

                        val errorDetail = ErrorDetail(
                            message = errorMessage,
                            statusCode = extractionsResult.responseStatusCode,
                            errorResponse = extractionsResult.errorResponse  // Already parsed!
                        )
                        errors.add(errorDetail)
                    }
                    is Resource.Cancelled -> {
                        LOG.warn("Get extractions cancelled for document ${emptyDocument.id}")
                    }
                }
            }
        }

        jobs.awaitAll()
        invoicesLocalDataSource.refreshInvoices(documentsWithExtractions)

        if (errors.isNotEmpty()) {
            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Failure(errors)
        } else {
            _uploadHardcodedInvoicesStateFlow.value = UploadHardcodedInvoicesState.Success
        }
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
                try {
                    val isPayable = giniHealth.checkIfDocumentIsPayable(document.id)
                    val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                        document,
                        extractionsResource.data,
                        isPayable
                    )
                    Pair(documentWithExtractions, extractionsResource)
                } catch (e: GiniHealthException) {
                    val documentWithExtractions = DocumentWithExtractions.fromDocumentAndExtractions(
                        document,
                        extractionsResource.data,
                        false
                    )
                    Pair(documentWithExtractions, extractionsResource)
                }
            }
            is Resource.Error -> {
                // Error is already parsed by Resource! Log detailed error information
                val errorMessage = extractionsResource.errorResponse?.items?.firstOrNull()?.message
                    ?: extractionsResource.exception?.message
                    ?: "Unknown error"
                LOG.error("Error getting extractions for document ${document.id}: $errorMessage")
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
    val errorResponse: net.gini.android.core.api.response.ErrorResponse? = null,
    val errorCode: String? = errorResponse?.items?.firstOrNull()?.code,
    val requestId: String? = errorResponse?.requestId
)

