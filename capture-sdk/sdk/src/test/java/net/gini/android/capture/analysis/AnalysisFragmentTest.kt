package net.gini.android.capture.analysis

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.analysis.warning.WarningBottomSheet
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class AnalysisFragmentTest {

    @Test
    fun `triggers Cancel event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setEventTracker(eventTracker).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(mock())
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)

        val bundle = Bundle().apply {
            putParcelable("GC_ARGS_DOCUMENT", mock<ImageDocument>().apply {
                whenever(isReviewable).thenReturn(true)
                whenever(type).thenReturn(Document.Type.IMAGE)
            })
            putString("GC_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE", "")
        }
        FragmentScenario.launchInContainer(fragmentClass = AnalysisFragment::class.java, fragmentArgs = bundle,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return AnalysisFragment().apply {
                        setListener(mock())
                        setBankSDKBridge(mock())
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                // The fragment’s view has just been created
                                Navigation.setViewNavController(fragment.requireView(), mock())
                            }
                        }
                    }
                }
            }).use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            // When
            scenario.onFragment { fragment ->
                fragment.requireActivity().onBackPressedDispatcher.onBackPressed()

                // Then
                verify(eventTracker).onAnalysisScreenEvent(Event(AnalysisScreenEvent.CANCEL))
            }
        }
    }
    @Test
    fun `showWarning displays bottom sheet with correct type and listener`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setEventTracker(eventTracker).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(mock())

        val bundle = Bundle().apply {
            putParcelable("GC_ARGS_DOCUMENT", mock<ImageDocument>().apply {
                whenever(isReviewable).thenReturn(true)
                whenever(type).thenReturn(Document.Type.IMAGE)
            })
            putString("GC_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE", "")
        }

        val mockOnProceed = mock<Runnable>()

        FragmentScenario.launchInContainer(
            fragmentClass = AnalysisFragment::class.java,
            fragmentArgs = bundle,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return AnalysisFragment().apply {
                        setListener(mock())
                        setBankSDKBridge(mock())
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                Navigation.setViewNavController(fragment.requireView(), mock())
                            }
                        }
                    }
                }
            }
        ).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            // When
            scenario.onFragment { fragment ->
                fragment.showWarning(WarningType.DOCUMENT_MARKED_AS_PAID, mockOnProceed)

                // Execute pending transactions to ensure the bottom sheet is added
                val fragmentManager = fragment.requireActivity().supportFragmentManager
                fragmentManager.executePendingTransactions()

                // Then
                val warningSheet = fragmentManager.findFragmentByTag("WarningBottomSheet")

                assertThat(warningSheet).isNotNull()
                assertThat(warningSheet).isInstanceOf(WarningBottomSheet::class.java)
            }
        }
    }

}
