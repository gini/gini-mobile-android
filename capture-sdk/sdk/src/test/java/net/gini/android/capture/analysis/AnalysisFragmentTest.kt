package net.gini.android.capture.analysis

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.analysis.warning.WarningBottomSheet
import net.gini.android.capture.analysis.warning.WarningType
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Shadows

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class AnalysisFragmentTest {

    private lateinit var koinTestModule: Module
    private lateinit var configurationProvider: GiniBankConfigurationProvider

    @Before
    fun setUp() {
        // GiniBankConfigurationProvider is registered in the SDK's isolated Koin context by the
        // Bank SDK's DI bridge, so a capture-sdk unit test has to provide its own definition.
        // The Analysis presenter resolves it on start() to decide whether the Gini ingredient
        // brand element is shown.
        configurationProvider = GiniBankConfigurationProvider()
        koinTestModule = module {
            single { configurationProvider }
        }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
    }

    private fun enableIngredientBrandOnAnalysis() {
        configurationProvider.update { it.copy(ingredientBrandScreens = setOf("Analysis")) }
    }

    private fun loadingIndicatorContainer(fragment: AnalysisFragment) =
        fragment.requireView()
            .findViewById<InjectedViewContainer<CustomLoadingIndicatorAdapter>>(
                R.id.gc_injected_loading_indicator_container
            )

    /**
     * The indicator actually on screen.
     *
     * The screen always binds an [net.gini.android.capture.ingredientbrand.IngredientBrandLoadingIndicatorAdapter],
     * which hosts both the Gini mark and the integrator's indicator and shows whichever the client
     * configuration currently calls for — so the interesting view is the visible grandchild, not
     * the container's direct child.
     */
    private fun loadingIndicatorChild(fragment: AnalysisFragment): View? {
        val container = loadingIndicatorContainer(fragment)
        val host = (if (container.childCount > 0) container.getChildAt(0) else null) as? ViewGroup
            ?: return null
        return (0 until host.childCount).map { host.getChildAt(it) }
            .firstOrNull { it.visibility == View.VISIBLE }
    }

    /** Hides and re-shows the indicator, which is when the branding choice is re-made. */
    private fun showLoadingIndicatorAgain(fragment: AnalysisFragment) {
        val adapter = loadingIndicatorContainer(fragment)
            .injectedViewAdapterHolder?.viewAdapterInstance?.viewAdapter ?: return
        adapter.onHidden()
        adapter.onVisible()
    }

    private open class RecordingLoadingIndicatorAdapter : CustomLoadingIndicatorAdapter {
        override fun onCreateView(container: ViewGroup): View = View(container.context)
        override fun onVisible() = Unit
        override fun onHidden() = Unit
        override fun onDestroy() = Unit
    }

    /**
     * Requirement: the client configuration is the only switch for the ingredient brand loading
     * indicator. There is no public API an integrator could call to turn it on.
     */
    @Test
    fun `shows the Gini loading indicator when ingredientBrandScreens contains Analysis`() {
        enableIngredientBrandOnAnalysis()

        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(loadingIndicatorChild(fragment))
                    .isInstanceOf(ImageView::class.java)
            }
        }
    }

    /** Clients without ingredient branding must see no change at all. */
    @Test
    fun `shows the default loading indicator when ingredientBrandScreens is empty`() {
        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(loadingIndicatorChild(fragment))
                    .isNotInstanceOf(ImageView::class.java)
            }
        }
    }

    /**
     * Regression for the review finding on PR #993.
     *
     * On the "open with" path `GiniCaptureFragment` navigates straight to Analysis, so on a cold
     * first launch this screen is built before `/configurations` has answered and
     * `ingredientBrandScreens` is still empty. The screen used to decide once at view creation and
     * keep the integrator's indicator for good; it now re-reads the configuration every time the
     * indicator is shown, so branding that arrives late is still honoured.
     */
    @Test
    fun `picks up ingredient branding that arrives after the screen was created`() {
        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                // Configuration has not arrived yet.
                assertThat(loadingIndicatorChild(fragment))
                    .isNotInstanceOf(ImageView::class.java)

                enableIngredientBrandOnAnalysis()
                showLoadingIndicatorAgain(fragment)

                assertThat(loadingIndicatorChild(fragment))
                    .isInstanceOf(ImageView::class.java)
            }
        }
    }

    /**
     * The ingredient brand is not a customisation point. An adapter injected with
     * GiniCapture.Builder.setLoadingIndicatorAdapter() must not even be created on this screen
     * while ingredient branding is on, so a bank cannot replace or suppress the Gini mark.
     */
    @Test
    fun `ignores a custom loading indicator adapter while ingredient branding is on`() {
        enableIngredientBrandOnAnalysis()
        val customAdapter = spy<CustomLoadingIndicatorAdapter>(RecordingLoadingIndicatorAdapter())

        launchFragment(mock(), customAdapter).use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(loadingIndicatorChild(fragment))
                    .isInstanceOf(ImageView::class.java)
                verify(customAdapter, never()).onCreateView(any())
                verify(customAdapter, never()).onVisible()
            }
        }
    }

    /** The same integrator adapter keeps working when the client has no ingredient branding. */
    @Test
    fun `uses the custom loading indicator adapter while ingredient branding is off`() {
        val customAdapter = spy<CustomLoadingIndicatorAdapter>(RecordingLoadingIndicatorAdapter())

        launchFragment(mock(), customAdapter).use { scenario ->
            scenario.onFragment { _ ->
                verify(customAdapter).onCreateView(any())
            }
        }
    }

    private fun poweredByGini(fragment: AnalysisFragment): View =
        fragment.requireView().findViewById(R.id.gc_powered_by_gini)

    private fun hintContainer(fragment: AnalysisFragment): View =
        fragment.requireView().findViewById(R.id.gc_analysis_hint_container)

    /** Runs the capture suggestion timer until the first tip has slid into view. */
    private fun showFirstCaptureSuggestion() {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
    }

    /** The badge is visible from the start, before any capture suggestion is shown. */
    @Test
    fun `shows the Powered by Gini badge before the first capture suggestion`() {
        enableIngredientBrandOnAnalysis()

        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(hintContainer(fragment).visibility).isNotEqualTo(View.VISIBLE)
                assertThat(poweredByGini(fragment).visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    /**
     * Matches iOS: the first capture suggestion takes the badge's place at the bottom of the
     * screen, and the badge does not come back while the tips keep cycling.
     */
    @Test
    fun `hides the Powered by Gini badge when the first capture suggestion is shown`() {
        enableIngredientBrandOnAnalysis()

        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                showFirstCaptureSuggestion()

                assertThat(hintContainer(fragment).visibility).isEqualTo(View.VISIBLE)
                assertThat(poweredByGini(fragment).visibility).isEqualTo(View.GONE)
            }
        }
    }

    /** `onResume` re-applies the badge visibility; it must not bring the badge back over a tip. */
    @Test
    fun `keeps the Powered by Gini badge hidden after the screen is resumed`() {
        enableIngredientBrandOnAnalysis()

        launchFragment(mock()).use { scenario ->
            scenario.onFragment { showFirstCaptureSuggestion() }

            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onFragment { fragment ->
                assertThat(poweredByGini(fragment).visibility).isEqualTo(View.GONE)
            }
        }
    }

    /**
     * A payment hint hides the capture suggestions again. The badge must still stay hidden behind
     * the bottom sheet when the screen is resumed, for example after the app was in the background.
     */
    @Test
    fun `keeps the Powered by Gini badge hidden behind a payment hint after the screen is resumed`() {
        enableIngredientBrandOnAnalysis()

        launchFragment(mock()).use { scenario ->
            scenario.onFragment { fragment ->
                showFirstCaptureSuggestion()
                fragment.fragmentImpl.showWarning(WarningType.PAYMENT_DUE_DATE, "01.01.2027", {}, null)
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                assertThat(hintContainer(fragment).visibility).isEqualTo(View.GONE)
            }

            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onFragment { fragment ->
                assertThat(poweredByGini(fragment).visibility).isEqualTo(View.GONE)
            }
        }
    }

    @After
    fun tearDown() {
        // Koin's unloadModules drops the overriding definition instead of restoring the previous
        // one, and getGiniCaptureKoin() is a process-wide isolated context — so the definition is
        // re-loaded for later test classes running in the same JVM.
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        getGiniCaptureKoin().loadModules(
            listOf(module { single { GiniBankConfigurationProvider() } })
        )
    }

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

    /**
     * Requirements 6 + 7 — for the scheduled payment state the primary CTA is "Schedule Payment"
     * (hand-off) and the secondary is "Proceed Anyway". Neither CTA cancels the transaction.
     */
    @Test
    fun `wires schedule to primary CTA and proceed to secondary CTA for SCHEDULE_PAYMENT warning`() {
        val cancelListener = mock<CancelListener>()
        launchFragment(cancelListener).use { scenario ->
            scenario.onFragment { fragment ->
                // When
                val proceeded = AtomicBoolean(false)
                val scheduled = AtomicBoolean(false)
                fragment.showWarning(
                    WarningType.SCHEDULE_PAYMENT,
                    "13.08.2026",
                    { proceeded.set(true) },
                    { scheduled.set(true) }
                )
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val sheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet

                // Then: primary = "Schedule Payment" hands off, without proceeding or cancelling
                sheet.dialog?.findViewById<View>(R.id.primary_button)?.performClick()
                Truth.assertThat(scheduled.get()).isTrue()
                Truth.assertThat(proceeded.get()).isFalse()
                verify(cancelListener, never()).onCancelFlow()
                // Complete the pending dismissal before showing the sheet again
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()

                // And: secondary = "Proceed Anyway" continues the flow
                scheduled.set(false)
                fragment.showWarning(
                    WarningType.SCHEDULE_PAYMENT,
                    "13.08.2026",
                    { proceeded.set(true) },
                    { scheduled.set(true) }
                )
                fragment.requireActivity().supportFragmentManager.executePendingTransactions()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                val secondSheet = fragment.requireActivity().supportFragmentManager
                    .findFragmentByTag("WarningBottomSheet") as WarningBottomSheet
                secondSheet.dialog?.findViewById<View>(R.id.secondary_button)?.performClick()
                Truth.assertThat(proceeded.get()).isTrue()
                Truth.assertThat(scheduled.get()).isFalse()
                verify(cancelListener, never()).onCancelFlow()
            }
        }
    }

    private fun launchFragment(
        cancelListener: CancelListener,
        loadingIndicatorAdapter: CustomLoadingIndicatorAdapter? = null,
    ): FragmentScenario<AnalysisFragment> {
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().context)
            .setGiniCaptureNetworkService(mock())
            .also { builder ->
                loadingIndicatorAdapter?.let { builder.setLoadingIndicatorAdapter(it) }
            }.build()
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
