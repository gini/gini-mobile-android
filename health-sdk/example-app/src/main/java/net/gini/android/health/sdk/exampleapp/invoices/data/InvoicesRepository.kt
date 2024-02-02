package net.gini.android.health.sdk.exampleapp.invoices.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.core.api.MediaTypes
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions

class InvoicesRepository(
    private val giniHealthAPI: GiniHealthAPI,
    private val hardcodedInvoicesLocalDataSource: HardcodedInvoicesLocalDataSource,
    private val invoicesLocalDataSource: InvoicesLocalDataSource
) {

    private val _uploadHardcodedInvoicesState: MutableStateFlow<UploadHardcodedInvoicesState> = MutableStateFlow(
        UploadHardcodedInvoicesState.Idle
    )
    val uploadHardcodedInvoicesState = _uploadHardcodedInvoicesState.asStateFlow()

    val invoicesFlow = invoicesLocalDataSource.invoicesFlow

    suspend fun loadInvoicesWithExtractions() {
        invoicesLocalDataSource.loadInvoicesWithExtractions()
    }

    suspend fun uploadHardcodedInvoices() {
        _uploadHardcodedInvoicesState.value = UploadHardcodedInvoicesState.Loading

        val documentsWithExtractions = mutableListOf<DocumentWithExtractions>()

        val hardcodedInvoices = hardcodedInvoicesLocalDataSource.getHardcodedInvoices()
        val createdResources = hardcodedInvoices.map { invoiceBytes ->
            var document: Document? = null
            var extractionsContainer: ExtractionsContainer? = null
            giniHealthAPI.documentManager.createPartialDocument(
                invoiceBytes,
                MediaTypes.IMAGE_JPEG
            ).mapSuccess { partialDocumentResource ->
                giniHealthAPI.documentManager.createCompositeDocument(listOf(partialDocumentResource.data))
            }.mapSuccess { compositeDocumentResource ->
                document = compositeDocumentResource.data
                giniHealthAPI.documentManager.getAllExtractionsWithPolling(compositeDocumentResource.data)
            }.mapSuccess { extractionsResource ->
                extractionsContainer = extractionsResource.data
                documentsWithExtractions.add(DocumentWithExtractions.fromDocumentAndExtractions(document!!, extractionsContainer!!))
                extractionsResource
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
            _uploadHardcodedInvoicesState.value = UploadHardcodedInvoicesState.Success
        } else {
            _uploadHardcodedInvoicesState.value = UploadHardcodedInvoicesState.Failure(errors)
        }

        _uploadHardcodedInvoicesState.value = UploadHardcodedInvoicesState.Idle
    }
}

sealed class UploadHardcodedInvoicesState {
    object Idle : UploadHardcodedInvoicesState()
    object Loading : UploadHardcodedInvoicesState()
    object Success : UploadHardcodedInvoicesState()
    data class Failure(val errors: List<String>) : UploadHardcodedInvoicesState()
}
