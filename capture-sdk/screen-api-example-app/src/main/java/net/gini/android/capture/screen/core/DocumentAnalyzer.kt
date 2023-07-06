package net.gini.android.capture.screen.core

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

/**
 *
 */
class DocumentAnalyzer @JvmOverloads internal constructor(
    private val documentManager: DocumentManager<*, *>,
    coroutineContext: CoroutineContext = Dispatchers.Main
) {
    private val coroutineScope = CoroutineScope(coroutineContext)

    var giniApiDocument: Document? = null
        private set

    private var extractionsResource: Resource<out ExtractionsContainer>? = null

    var listener: Listener? = null

    val isCompleted: Boolean
        get() = extractionsResource != null

    @get:JvmName("isCancelled")
    var cancelled: Boolean = false
        private set

    fun analyze(document: net.gini.android.capture.Document) {
        coroutineScope.launch {
            val extractionsResource = documentManager.createPartialDocument(
                document = document.data!!,
                contentType = document.mimeType,
                filename = null,
                documentType = null
            ).mapSuccess { partialDocumentResource ->
                Log.d("gini-api", "Partial document created: " + partialDocumentResource.data.id)
                Log.d(
                    "gini-api", "Creating composite document for partial document: " + partialDocumentResource.data.id
                )
                documentManager.createCompositeDocument(listOf(partialDocumentResource.data))
            }.mapSuccess { compositeDocumentResource ->
                Log.d("gini-api", "Composite document created: " + compositeDocumentResource.data.id)
                giniApiDocument = compositeDocumentResource.data
                Log.d(
                    "gini-api", "Getting extractions for composite document: " + compositeDocumentResource.data.id
                )
                documentManager.getAllExtractionsWithPolling(compositeDocumentResource.data)
            }
            when (extractionsResource) {
                is Resource.Success -> {
                    Log.d("gini-api", "Analysis completed for document: ${giniApiDocument?.id}")
                    listener?.onExtractionsReceived(extractionsResource.data.specificExtractions)
                }
                is Resource.Error -> {
                    Log.d("gini-api", "Analysis failed for document ${giniApiDocument?.id}: ${extractionsResource!!.message}")
                    listener?.onException(Exception(extractionsResource.message, extractionsResource.exception))
                }
                is Resource.Cancelled -> {}
            }
            this@DocumentAnalyzer.extractionsResource = extractionsResource
        }

    }

    fun cancel() {
        cancelled = true
        listener = null
        coroutineScope.coroutineContext.cancelChildren()
    }

    interface Listener {
        fun onException(exception: Exception)
        fun onExtractionsReceived(extractions: Map<String, SpecificExtraction>)
    }
}