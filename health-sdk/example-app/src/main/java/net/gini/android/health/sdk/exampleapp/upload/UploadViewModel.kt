package net.gini.android.health.sdk.exampleapp.upload

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.core.api.MediaTypes
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.util.getBytes

class UploadViewModel(
    private val giniHealthAPI: GiniHealthAPI,
    val giniHealth: GiniHealth,
) : ViewModel() {
    private val _uploadState: MutableStateFlow<UploadState> = MutableStateFlow(UploadState.Loading)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun uploadDocuments(contentResolver: ContentResolver, pageUris: List<Uri>) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val documentPages = pageUris.map { pageUri ->
                    val stream = contentResolver.openInputStream(pageUri)
                    check(stream != null) { "ContentResolver failed" }
                    giniHealthAPI.documentManager.createPartialDocument(
                        stream.getBytes(),
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(pageUri)) ?: MediaTypes.IMAGE_JPEG
                    )
                }
                val document = giniHealthAPI.documentManager.createCompositeDocument(documentPages)
                val polledDocument = giniHealthAPI.documentManager.pollDocument(document)
                _uploadState.value = UploadState.Success(polledDocument.id)
                setDocumentForReview(polledDocument.id)
            } catch (throwable: Throwable) {
                _uploadState.value = UploadState.Failure(throwable)
            }
        }
    }

    private fun setDocumentForReview(documentId: String) {
        viewModelScope.launch {
            giniHealth.setDocumentForReview(documentId)
        }
    }

    sealed class UploadState {
        object Loading : UploadState()
        class Success(val documentId: String) : UploadState()
        class Failure(val throwable: Throwable) : UploadState()
    }
}