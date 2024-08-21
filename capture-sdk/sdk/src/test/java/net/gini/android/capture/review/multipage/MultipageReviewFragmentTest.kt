package net.gini.android.capture.review.multipage

import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.*
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.internal.network.NetworkRequestResult
import net.gini.android.capture.internal.network.NetworkRequestsManager
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.*
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`


/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class MultipageReviewFragmentTest {

    @After
    fun after() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `triggers Back event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(mock())
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)

        FragmentScenario.launchInContainer(fragmentClass = MultiPageReviewFragment::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            // When
            scenario.onFragment { fragment ->
                fragment.setCancelListener(mock())
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        // The fragment’s view has just been created
                        Navigation.setViewNavController(fragment.requireView(), mock())
                    }
                }
                try {
                    fragment.requireActivity().onBackPressedDispatcher.onBackPressed()
                } catch (e: IllegalStateException) {
                    // The only exception we can get must be related to the NavController
                    Truth.assertThat(e.message).contains("NavController")
                }

                // Then
                verify(eventTracker).onReviewScreenEvent(Event(ReviewScreenEvent.BACK))
            }
        }
    }

    @Test
    fun `triggers Next event`() {
            // Given
            val eventTracker = spy<EventTracker>()
            GiniCapture.Builder().setEventTracker(eventTracker).build()
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(mock())
            UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
            FragmentScenario.launchInContainer(fragmentClass = MultiPageReviewFragment::class.java).use { scenario ->
                scenario.moveToState(Lifecycle.State.STARTED)

                // When
                scenario.onFragment { fragment ->
                    try {
                        fragment.onNextButtonClicked()
                    } catch (e: IllegalStateException) {
                        // The only exception we can get must be related to the NavController
                        Truth.assertThat(e.message).contains("NavController")
                    }

                    // Then
                    verify(eventTracker).onReviewScreenEvent(Event(ReviewScreenEvent.NEXT))
                }
            }
    }

    @Test
    fun `triggers Upload Error event`() {
        // Given
        // Note: Use FragmentScenario in the future
        val fragment = mock<MultiPageReviewFragment>()

        // TODO: use FragmentScenario to fix the error
        `when`(fragment.activity).thenReturn(mock())
        fragment.mMultiPageDocument = mock()
        fragment.mDocumentUploadResults = mock()

        `when`(fragment.uploadDocument(any())).thenCallRealMethod()

        val exception = RuntimeException("error message")

        val future = CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>()
        future.completeExceptionally(exception)

        val networkRequestsManager = mock<NetworkRequestsManager>()
        `when`(networkRequestsManager.upload(any(), any())).thenReturn(future)

        val internal = mock<GiniCapture.Internal>()
        `when`(internal.networkRequestsManager).thenReturn(networkRequestsManager)

        val giniCapture = mock<GiniCapture>()
        GiniCaptureHelper.setGiniCaptureInstance(giniCapture)

        `when`(giniCapture.internal()).thenReturn(internal)

        val eventTracker = spy<EventTracker>()
        `when`(giniCapture.internal().eventTracker).thenReturn(eventTracker)

        // When
        fragment.uploadDocument(ImageDocumentFake())

        // Then
        val errorDetails = mapOf(
                MESSAGE to exception.message,
                ERROR_OBJECT to exception
        )
        Mockito.verify(eventTracker).onReviewScreenEvent(Event(ReviewScreenEvent.UPLOAD_ERROR, errorDetails))
    }
}