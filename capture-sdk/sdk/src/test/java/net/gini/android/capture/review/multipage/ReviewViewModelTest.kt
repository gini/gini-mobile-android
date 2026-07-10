package net.gini.android.capture.review.multipage

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import jersey.repackaged.jsr166e.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.cache.DocumentDataMemoryCache
import net.gini.android.capture.internal.cache.PhotoMemoryCache
import net.gini.android.capture.internal.network.NetworkRequestResult
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Unit tests for [ReviewViewModel].
 *
 * The upload related tests were ported from the former [MultiPageReviewFragment] tests which
 * exercised the fragment's inline upload logic.
 */
@RunWith(AndroidJUnit4::class)
class ReviewViewModelTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun createViewModel(): ReviewViewModel {
        return ReviewViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    private fun collectEvents(viewModel: ReviewViewModel): MutableList<ReviewEvent> {
        val events = CopyOnWriteArrayList<ReviewEvent>()
        CoroutineScope(Dispatchers.Unconfined).launch {
            viewModel.events.collect { events.add(it) }
        }
        return events
    }

    private fun setGiniCaptureInstanceWithNetworkManager(
        networkRequestsManager: NetworkRequestsManager?
    ): EventTracker {
        val eventTracker = spy<EventTracker>()
        val internal = mock<GiniCapture.Internal>()
        whenever(internal.networkRequestsManager).thenReturn(networkRequestsManager)
        whenever(internal.eventTracker).thenReturn(eventTracker)
        whenever(internal.documentDataMemoryCache).thenReturn(mock<DocumentDataMemoryCache>())
        whenever(internal.photoMemoryCache).thenReturn(mock<PhotoMemoryCache>())
        whenever(internal.imageMultiPageDocumentMemoryStore).thenReturn(mock())
        val giniCapture = mock<GiniCapture>()
        whenever(giniCapture.internal()).thenReturn(internal)
        GiniCaptureHelper.setGiniCaptureInstance(giniCapture)
        return eventTracker
    }

    @Test
    fun `triggers Upload Error event`() {
        // Given
        val exception = RuntimeException("error message")
        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.completeExceptionally(exception)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)

        val eventTracker = setGiniCaptureInstanceWithNetworkManager(networkRequestsManager)

        val viewModel = createViewModel()
        viewModel.multiPageDocument = mock()

        // When
        viewModel.uploadDocument(ImageDocumentFake())

        // Then
        val errorDetails = mapOf(
            MESSAGE to exception.message,
            ERROR_OBJECT to exception
        )
        verify(eventTracker).onReviewScreenEvent(
            Event(ReviewScreenEvent.UPLOAD_ERROR, errorDetails)
        )
    }

    @Test
    fun `emits NavigateToError event when upload fails`() {
        // Given
        val exception = RuntimeException("error message")
        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.completeExceptionally(exception)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)

        setGiniCaptureInstanceWithNetworkManager(networkRequestsManager)

        val viewModel = createViewModel()
        viewModel.multiPageDocument = mock()
        val events = collectEvents(viewModel)

        // When
        viewModel.uploadDocument(ImageDocumentFake())

        // Then
        Truth.assertThat(events.filterIsInstance<ReviewEvent.NavigateToError>()).isNotEmpty()
        Truth.assertThat(viewModel.uiState.value.uploadIndicatorVisible).isFalse()
    }

    @Test
    fun `enables next button and hides upload indicator when all pages uploaded successfully`() {
        // Given
        val document = ImageDocumentFake()
        val multiPageDocument = mock<ImageMultiPageDocument> {
            on { documents } doReturn mutableListOf(document)
        }
        val requestResult = mock<NetworkRequestResult<GiniCaptureDocument>>()
        val future = CompletableFuture.completedFuture(requestResult)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)

        setGiniCaptureInstanceWithNetworkManager(networkRequestsManager)

        val viewModel = createViewModel()
        viewModel.multiPageDocument = multiPageDocument

        // When
        viewModel.uploadDocument(document)

        // Then
        Truth.assertThat(viewModel.documentUploadResults[document.id]).isTrue()
        Truth.assertThat(viewModel.uiState.value.nextButtonEnabled).isTrue()
        Truth.assertThat(viewModel.uiState.value.uploadIndicatorVisible).isFalse()
    }

    @Test
    fun `disables next button while an upload is pending`() {
        // Given
        val document = ImageDocumentFake()
        val otherDocument = ImageDocumentFake()
        val multiPageDocument = mock<ImageMultiPageDocument> {
            on { documents } doReturn mutableListOf(document, otherDocument)
        }
        val requestResult = mock<NetworkRequestResult<GiniCaptureDocument>>()
        val future = CompletableFuture.completedFuture(requestResult)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)

        setGiniCaptureInstanceWithNetworkManager(networkRequestsManager)

        val viewModel = createViewModel()
        viewModel.multiPageDocument = multiPageDocument
        // The other page hasn't been uploaded yet
        viewModel.documentUploadResults[otherDocument.id] = false

        // When
        viewModel.uploadDocument(document)

        // Then
        Truth.assertThat(viewModel.uiState.value.nextButtonEnabled).isFalse()
    }

    @Test
    fun `emits PageDeleted with wasLastPage when the only page is deleted`() {
        // Given
        val document = ImageDocumentFake()
        val multiPageDocument = mock<ImageMultiPageDocument> {
            on { documents } doReturn mutableListOf(document)
        }
        setGiniCaptureInstanceWithNetworkManager(null)

        val viewModel = createViewModel()
        viewModel.multiPageDocument = multiPageDocument
        viewModel.documentUploadResults[document.id] = true
        val events = collectEvents(viewModel)

        // When
        viewModel.onDeleteDocument(document)

        // Then
        val event = events.filterIsInstance<ReviewEvent.PageDeleted>().first()
        Truth.assertThat(event.deletedPosition).isEqualTo(0)
        Truth.assertThat(event.wasLastPage).isTrue()
        Truth.assertThat(viewModel.documentUploadResults).doesNotContainKey(document.id)
        Truth.assertThat(viewModel.uiState.value.nextButtonEnabled).isFalse()
    }

    @Test
    fun `initMultiPageDocument returns true when the document has new pages`() {
        // Given
        val document = ImageDocumentFake()
        val multiPageDocument = mock<ImageMultiPageDocument> {
            on { documents } doReturn mutableListOf(document)
        }
        val internal = mock<GiniCapture.Internal>()
        val memoryStore = mock<net.gini.android.capture.internal.document.ImageMultiPageDocumentMemoryStore>()
        whenever(memoryStore.multiPageDocument).thenReturn(multiPageDocument)
        whenever(internal.imageMultiPageDocumentMemoryStore).thenReturn(memoryStore)
        val giniCapture = mock<GiniCapture>()
        whenever(giniCapture.internal()).thenReturn(internal)
        GiniCaptureHelper.setGiniCaptureInstance(giniCapture)

        val viewModel = createViewModel()

        // When
        val hasNewPages = viewModel.initMultiPageDocument()

        // Then
        Truth.assertThat(hasNewPages).isTrue()
        Truth.assertThat(viewModel.documentUploadResults[document.id]).isFalse()

        // Calling it again without new pages returns false
        Truth.assertThat(viewModel.initMultiPageDocument()).isFalse()
    }
}
