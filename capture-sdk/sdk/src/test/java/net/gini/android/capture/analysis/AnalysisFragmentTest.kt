package net.gini.android.capture.analysis

import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.atomic.AtomicBoolean
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.analysis.warning.WarningBottomSheet
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

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
    fun `wires proceed to primary CTA and cancel to secondary CTA for PAYMENT_DUE_DATE warning`() {
        val cancelListener = mock<CancelListener>()
        launchFragment(cancelListener).use { scenario ->
            scenario.onFragment { fragment ->
                // When
                val proceeded = AtomicBoolean(false)
                fragment.showWarning(WarningType.PAYMENT_DUE_DATE, "13.08.2026") {
                    proceeded.set(true)
                }
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val sheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet

                // Then: primary = "Proceed Anyway" continues the flow
                sheet.dialog?.findViewById<View>(R.id.primary_button)?.performClick()
                Truth.assertThat(proceeded.get()).isTrue()
                verify(cancelListener, never()).onCancelFlow()
                // Complete the pending dismissal before showing the sheet again
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()

                // And: secondary = "Cancel Transfer" cancels the transaction
                fragment.showWarning(WarningType.PAYMENT_DUE_DATE, "13.08.2026") {}
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val secondSheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet
                secondSheet.dialog?.findViewById<View>(R.id.secondary_button)?.performClick()
                verify(cancelListener).onCancelFlow()
            }
        }
    }

    @Test
    fun `wires cancel to primary CTA and proceed to secondary CTA for DOCUMENT_MARKED_AS_PAID warning`() {
        val cancelListener = mock<CancelListener>()
        launchFragment(cancelListener).use { scenario ->
            scenario.onFragment { fragment ->
                // When
                val proceeded = AtomicBoolean(false)
                fragment.showWarning(WarningType.DOCUMENT_MARKED_AS_PAID, null) {
                    proceeded.set(true)
                }
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val sheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet

                // Then: primary = "Cancel Transfer" cancels the transaction
                sheet.dialog?.findViewById<View>(R.id.primary_button)?.performClick()
                verify(cancelListener).onCancelFlow()
                Truth.assertThat(proceeded.get()).isFalse()
                // Complete the pending dismissal before showing the sheet again
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()

                // And: secondary = "Proceed Anyway" continues the flow
                fragment.showWarning(WarningType.DOCUMENT_MARKED_AS_PAID, null) {
                    proceeded.set(true)
                }
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val secondSheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet
                secondSheet.dialog?.findViewById<View>(R.id.secondary_button)?.performClick()
                Truth.assertThat(proceeded.get()).isTrue()
            }
        }
    }

    private fun launchFragment(cancelListener: CancelListener): FragmentScenario<AnalysisFragment> {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock()).build()
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore
            .setMultiPageDocument(mock())
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)

        val bundle = Bundle().apply {
            putParcelable("GC_ARGS_DOCUMENT", mock<ImageDocument>().apply {
                whenever(isReviewable).thenReturn(true)
                whenever(type).thenReturn(Document.Type.IMAGE)
            })
            putString("GC_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE", "")
        }
        return FragmentScenario.launchInContainer(
            fragmentClass = AnalysisFragment::class.java,
            fragmentArgs = bundle,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return AnalysisFragment().apply {
                        setListener(mock())
                        setBankSDKBridge(mock())
                        setCancelListener(cancelListener)
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                // The fragment’s view has just been created
                                Navigation.setViewNavController(fragment.requireView(), mock())
                            }
                        }
                    }
                }
            }
        )
    }
}
