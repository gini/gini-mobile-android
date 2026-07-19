package net.gini.android.capture.noresults

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoResultsViewModelTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun document(
        source: Document.Source = Document.Source.newCameraSource(),
        importMethod: Document.ImportMethod = Document.ImportMethod.NONE,
        type: Document.Type = Document.Type.IMAGE
    ): Document = mock<Document>().also {
        whenever(it.source).thenReturn(source)
        whenever(it.importMethod).thenReturn(importMethod)
        whenever(it.type).thenReturn(type)
        whenever(it.id).thenReturn("document-id")
    }

    private fun imageDocument(
        source: Document.Source = Document.Source.newCameraSource(),
        importMethod: Document.ImportMethod = Document.ImportMethod.NONE
    ): ImageDocument = mock<ImageDocument>().also {
        whenever(it.source).thenReturn(source)
        whenever(it.importMethod).thenReturn(importMethod)
    }

    private fun multiPageDocument(
        vararg documents: ImageDocument
    ): ImageMultiPageDocument = mock<ImageMultiPageDocument>().also {
        whenever(it.documents).thenReturn(documents.toList())
        whenever(it.type).thenReturn(Document.Type.IMAGE_MULTI_PAGE)
        whenever(it.id).thenReturn("document-id")
    }

    @Test
    fun `allows retaking images for documents from the camera screen`() {
        // When
        val viewModel = NoResultsViewModel(document())

        // Then
        assertThat(viewModel.uiState.value!!.allowRetakeImages).isTrue()
    }

    @Test
    fun `does not allow retaking images for documents received via open with`() {
        // When
        val viewModel = NoResultsViewModel(
            document(importMethod = Document.ImportMethod.OPEN_WITH)
        )

        // Then
        assertThat(viewModel.uiState.value!!.allowRetakeImages).isFalse()
    }

    @Test
    fun `does not allow retaking images for documents from an external source`() {
        // When
        val viewModel = NoResultsViewModel(
            document(source = Document.Source.newExternalSource())
        )

        // Then
        assertThat(viewModel.uiState.value!!.allowRetakeImages).isFalse()
    }

    @Test
    fun `allows retaking images when all pages are from the camera screen`() {
        // When
        val viewModel = NoResultsViewModel(
            multiPageDocument(imageDocument(), imageDocument())
        )

        // Then
        assertThat(viewModel.uiState.value!!.allowRetakeImages).isTrue()
    }

    @Test
    fun `does not allow retaking images when a page was imported`() {
        // When
        val viewModel = NoResultsViewModel(
            multiPageDocument(
                imageDocument(),
                imageDocument(source = Document.Source.newExternalSource())
            )
        )

        // Then
        assertThat(viewModel.uiState.value!!.allowRetakeImages).isFalse()
    }

    @Test
    fun `shows the QR code title for QR code documents`() {
        // When
        val viewModel = NoResultsViewModel(document(type = Document.Type.QRCode))

        // Then
        assertThat(viewModel.uiState.value!!.showQrCodeTitle).isTrue()
    }

    @Test
    fun `does not show the QR code title for other documents`() {
        // When
        val viewModel = NoResultsViewModel(document(type = Document.Type.IMAGE))

        // Then
        assertThat(viewModel.uiState.value!!.showQrCodeTitle).isFalse()
    }

    @Test
    fun `retake images emits the navigate to camera event`() {
        // Given
        val viewModel = NoResultsViewModel(document())

        // When
        viewModel.onRetakeImagesClicked()

        // Then
        assertThat(viewModel.events.value!!.getContentIfNotHandled())
            .isEqualTo(NoResultsViewEvent.NavigateToCamera)
    }

    @Test
    fun `close emits the navigate to camera event`() {
        // Given
        val viewModel = NoResultsViewModel(document())

        // When
        viewModel.onCloseClicked()

        // Then
        assertThat(viewModel.events.value!!.getContentIfNotHandled())
            .isEqualTo(NoResultsViewEvent.NavigateToCamera)
    }

    @Test
    fun `enter manually emits the enter manually event`() {
        // Given
        val viewModel = NoResultsViewModel(document())

        // When
        viewModel.onEnterManuallyClicked()

        // Then
        assertThat(viewModel.events.value!!.getContentIfNotHandled())
            .isEqualTo(NoResultsViewEvent.EnterManually)
    }

    @Test
    fun `events are consumed only once`() {
        // Given
        val viewModel = NoResultsViewModel(document())

        // When
        viewModel.onCloseClicked()
        val event = viewModel.events.value!!

        // Then
        assertThat(event.getContentIfNotHandled()).isEqualTo(NoResultsViewEvent.NavigateToCamera)
        assertThat(event.getContentIfNotHandled()).isNull()
    }
}
