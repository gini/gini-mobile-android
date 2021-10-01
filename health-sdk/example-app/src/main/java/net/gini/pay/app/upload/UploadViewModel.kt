package net.gini.pay.app.upload

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.Gini
import net.gini.android.MediaTypes
import net.gini.pay.app.util.getBytes
import net.gini.pay.ginipaybusiness.GiniBusiness

class UploadViewModel(
    private val giniApi: Gini,
    val giniBusiness: GiniBusiness,
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
                    giniApi.documentManager.createPartialDocument(
                        stream.getBytes(),
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(pageUri)) ?: MediaTypes.IMAGE_JPEG
                    )
                }
                val document = giniApi.documentManager.createCompositeDocument(documentPages)
                val polledDocument = giniApi.documentManager.pollDocument(document)
                _uploadState.value = UploadState.Success(polledDocument.id)
                setDocumentForReview(polledDocument.id)
            } catch (throwable: Throwable) {
                _uploadState.value = UploadState.Failure(throwable)
            }
        }
    }

    private fun setDocumentForReview(documentId: String) {
        viewModelScope.launch {
            giniBusiness.setDocumentForReview(documentId)
        }
    }

    sealed class UploadState {
        object Loading : UploadState()
        class Success(val documentId: String) : UploadState()
        class Failure(val throwable: Throwable) : UploadState()
    }
}