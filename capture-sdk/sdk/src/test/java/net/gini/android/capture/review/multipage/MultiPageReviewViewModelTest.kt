package net.gini.android.capture.review.multipage

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.ImageMultiPageDocument
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

/**
 * Tests for [MultiPageReviewViewModel].
 *
 * The logic-level test cases were ported from the former `MultipageReviewFragmentTest` after the
 * MVP to MVVM migration of the Multi-Page Review screen. View method invocations are asserted
 * through the [MultiPageReviewViewModel.events], [MultiPageReviewViewModel.nextButtonEnabled] and
 * [MultiPageReviewViewModel.loadingIndicatorActive] LiveData instead of the fragment's views.
 */
@RunWith(AndroidJUnit4::class)
class MultiPageReviewViewModelTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun mockGiniCaptureWithEventTracker(): Pair<GiniCapture.Internal, EventTracker> {
        val internal = mock<GiniCapture.Internal>()
        val giniCapture = mock<GiniCapture>()
        whenever(giniCapture.internal()).thenReturn(internal)
        GiniCaptureHelper.setGiniCaptureInstance(giniCapture)

        val eventTracker = spy<EventTracker>()
        whenever(internal.eventTracker).thenReturn(eventTracker)
        return internal to eventTracker
    }

    @Test
    fun `triggers Back event when back was clicked`() {
        // Given
        val (_, eventTracker) = mockGiniCaptureWithEventTracker()
        val viewModel = MultiPageReviewViewModel()

        // When
        viewModel.onBackClicked()

        // Then
        verify(eventTracker).onReviewScreenEvent(Event(ReviewScreenEvent.BACK))
    }

    @Test
    fun `triggers Next event and navigates to the Analysis screen`() {
        // Given
        val (_, eventTracker) = mockGiniCaptureWithEventTracker()
        val viewModel = MultiPageReviewViewModel()
        val multiPageDocument = ImageMultiPageDocument(ImageDocumentFake())
        viewModel.multiPageDocument = multiPageDocument

        // When
        viewModel.onNextButtonClicked(true)

        // Then
        verify(eventTracker).onReviewScreenEvent(Event(ReviewScreenEvent.NEXT))

        val event = viewModel.pollEvent()
        Truth.assertThat(event)
            .isInstanceOf(MultiPageReviewViewEvent.NavigateToAnalysis::class.java)
        val navigateToAnalysis = event as MultiPageReviewViewEvent.NavigateToAnalysis
        Truth.assertThat(navigateToAnalysis.document).isEqualTo(multiPageDocument)
        Truth.assertThat(navigateToAnalysis.shouldSaveInvoicesLocally).isTrue()
    }

    @Test
    fun `triggers Upload Error event`() {
        // Given
        val (internal, eventTracker) = mockGiniCaptureWithEventTracker()

        val exception = RuntimeException("error message")

        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.completeExceptionally(exception)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)
        whenever(internal.networkRequestsManager).thenReturn(networkRequestsManager)

        val viewModel = MultiPageReviewViewModel()
        viewModel.multiPageDocument = mock()

        // When
        viewModel.uploadDocument(ImageDocumentFake(), mock<Activity>())

        // Then
        val errorDetails = mapOf(
            MESSAGE to exception.message,
            ERROR_OBJECT to exception
        )
        verify(eventTracker)
            .onReviewScreenEvent(Event(ReviewScreenEvent.UPLOAD_ERROR, errorDetails))
    }

    @Test
    fun `navigates to the Error screen when the upload failed`() {
        // Given
        val (internal, _) = mockGiniCaptureWithEventTracker()

        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.completeExceptionally(RuntimeException("error message"))

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)
        whenever(internal.networkRequestsManager).thenReturn(networkRequestsManager)

        val viewModel = MultiPageReviewViewModel()
        viewModel.multiPageDocument = mock()
        val imageDocument = ImageDocumentFake()

        // When
        viewModel.uploadDocument(imageDocument, mock<Activity>())

        // Then
        Truth.assertThat(viewModel.loadingIndicatorActive.value).isFalse()

        val event = viewModel.pollEvent()
        Truth.assertThat(event)
            .isInstanceOf(MultiPageReviewViewEvent.NavigateToUploadError::class.java)
        val navigateToUploadError = event as MultiPageReviewViewEvent.NavigateToUploadError
        Truth.assertThat(navigateToUploadError.document).isEqualTo(imageDocument)
    }

    @Test
    fun `enables the next button after all pages were uploaded successfully`() {
        // Given
        val (internal, _) = mockGiniCaptureWithEventTracker()

        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.complete(mock())

        val networkRequestsManager = mock<NetworkRequestsManager>()
        whenever(networkRequestsManager.upload(any(), any())).thenReturn(future)
        whenever(internal.networkRequestsManager).thenReturn(networkRequestsManager)

        val viewModel = MultiPageReviewViewModel()
        val imageDocument = ImageDocumentFake()
        viewModel.multiPageDocument = ImageMultiPageDocument(imageDocument)

        // When
        viewModel.uploadDocument(imageDocument, mock<Activity>())

        // Then
        Truth.assertThat(viewModel.documentUploadResults[imageDocument.id]).isTrue()
        Truth.assertThat(viewModel.nextButtonEnabled.value).isTrue()
        Truth.assertThat(viewModel.loadingIndicatorActive.value).isFalse()
    }

    @Test
    fun `disables the next button while pages were not uploaded`() {
        // Given
        val viewModel = MultiPageReviewViewModel()
        val imageDocument = ImageDocumentFake()
        viewModel.multiPageDocument = ImageMultiPageDocument(imageDocument)
        viewModel.documentUploadResults[imageDocument.id] = false

        // When
        viewModel.updateNextButtonVisibility()

        // Then
        Truth.assertThat(viewModel.nextButtonEnabled.value).isFalse()
    }

    @Test
    fun `deleting the last page requests the camera for the first page`() {
        // Given
        val viewModel = MultiPageReviewViewModel()
        val imageDocument = ImageDocumentFake()
        viewModel.multiPageDocument = ImageMultiPageDocument(imageDocument)
        viewModel.documentUploadResults[imageDocument.id] = true

        // When
        viewModel.onDeleteDocument(imageDocument)

        // Then
        Truth.assertThat(viewModel.multiPageDocument!!.documents).isEmpty()
        Truth.assertThat(viewModel.documentUploadResults).isEmpty()

        val firstEvent = viewModel.pollEvent()
        Truth.assertThat(firstEvent)
            .isInstanceOf(MultiPageReviewViewEvent.PageDeleted::class.java)
        Truth.assertThat((firstEvent as MultiPageReviewViewEvent.PageDeleted).deletedPosition)
            .isEqualTo(0)

        Truth.assertThat(viewModel.pollEvent())
            .isInstanceOf(MultiPageReviewViewEvent.NavigateToCameraForFirstPage::class.java)

        Truth.assertThat(viewModel.nextButtonEnabled.value).isFalse()
    }

    @Test
    fun `deleting a page keeps the review screen when other pages remain`() {
        // Given
        val viewModel = MultiPageReviewViewModel()
        val firstDocument = ImageDocumentFake()
        val secondDocument = ImageDocumentFake()
        val multiPageDocument = ImageMultiPageDocument(firstDocument)
        multiPageDocument.addDocument(secondDocument)
        viewModel.multiPageDocument = multiPageDocument
        viewModel.documentUploadResults[firstDocument.id] = true
        viewModel.documentUploadResults[secondDocument.id] = true

        // When
        viewModel.onDeleteDocument(secondDocument)

        // Then
        Truth.assertThat(viewModel.multiPageDocument!!.documents).containsExactly(firstDocument)

        val firstEvent = viewModel.pollEvent()
        Truth.assertThat(firstEvent)
            .isInstanceOf(MultiPageReviewViewEvent.PageDeleted::class.java)
        Truth.assertThat((firstEvent as MultiPageReviewViewEvent.PageDeleted).deletedPosition)
            .isEqualTo(1)

        Truth.assertThat(viewModel.pollEvent()).isNull()

        Truth.assertThat(viewModel.nextButtonEnabled.value).isTrue()
    }
}
