package net.gini.android.capture.review.multipage

import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.*
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.saveinvoiceslocally.GetSaveInvoicesLocallyFeatureEnabledUseCase
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module


/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class MultipageReviewFragmentTest {

    private val mockGetSaveInvoicesLocallyFeatureEnabledUseCase = mockk<GetSaveInvoicesLocallyFeatureEnabledUseCase>()
    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
        single { mockGetSaveInvoicesLocallyFeatureEnabledUseCase }
    }

    @Before
    fun setup() {
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
    }

    @After
    fun after() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    @Test
    fun `triggers Back event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(mock())
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
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
            every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true
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
    fun `process document view is shown`() {
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(
            mock()
        )
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        every { mockGetSaveInvoicesLocallyFeatureEnabledUseCase.invoke() } returns true

        FragmentScenario.launchInContainer(fragmentClass = MultiPageReviewFragment::class.java)
            .use { scenario ->
                scenario.moveToState(Lifecycle.State.RESUMED)

                scenario.onFragment { fragment ->
                    val processDocumentsWrapper =
                        fragment.requireView()
                            .findViewById<ConstraintLayout>(R.id.gc_process_documents_wrapper)
                    val nextButton =
                        fragment.requireView().findViewById<Button>(R.id.gc_button_next)
                    val addButton = fragment.requireView().findViewById<ImageView>(R.id.gc_add_page)

                    Truth.assertThat(processDocumentsWrapper.visibility).isEqualTo(View.VISIBLE)
                    Truth.assertThat(nextButton.visibility).isEqualTo(View.VISIBLE)
                    Truth.assertThat(addButton.visibility).isEqualTo(View.VISIBLE)
                }
            }
    }
}