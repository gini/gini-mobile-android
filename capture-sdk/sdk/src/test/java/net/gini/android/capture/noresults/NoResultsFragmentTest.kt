package net.gini.android.capture.noresults

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.Document
import net.gini.android.capture.EnterManuallyButtonListener
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.PdfDocumentFake
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.shadows.ShadowLooper

/**
 * Runs the real [NoResultsFragment] under Robolectric and checks where its buttons lead.
 */
@RunWith(AndroidJUnit4::class)
class NoResultsFragmentTest {

    private lateinit var navController: NavController
    private lateinit var enterManuallyButtonListener: EnterManuallyButtonListener

    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    @Before
    fun setUp() {
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
        GiniCapture.Builder().build()
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        navController = mock()
        enterManuallyButtonListener = mock()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    @Test
    fun `reports the enter manually button click to the listener`() {
        launchNoResultsFragment(ImageDocumentFake()) { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_button_no_results_enter_manually).performClick()

            verify(enterManuallyButtonListener).onEnterManuallyPressed()
        }
    }

    @Test
    fun `goes back to the camera when the retake images button is clicked`() {
        launchNoResultsFragment(ImageDocumentFake()) { fragment ->
            val retakeImagesButton =
                fragment.requireView().findViewById<View>(R.id.gc_button_no_results_retake_images)
            assertThat(retakeImagesButton.visibility).isEqualTo(View.VISIBLE)

            retakeImagesButton.performClick()

            verify(navController).navigate(NoResultsFragmentDirections.toCameraFragment())
        }
    }

    @Test
    fun `hides the retake images button for a document that was not taken with the camera`() {
        launchNoResultsFragment(PdfDocumentFake()) { fragment ->
            val retakeImagesButton =
                fragment.requireView().findViewById<View>(R.id.gc_button_no_results_retake_images)

            assertThat(retakeImagesButton.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `goes back to the camera when back is pressed`() {
        launchNoResultsFragment(ImageDocumentFake()) { fragment ->
            fragment.requireActivity().onBackPressedDispatcher.onBackPressed()

            verify(navController).navigate(NoResultsFragmentDirections.toCameraFragment())
        }
    }

    @Test
    fun `goes back to the camera when the top bar navigation button is clicked`() {
        launchNoResultsFragment(ImageDocumentFake()) { fragment ->
            ShadowLooper.idleMainLooper()
            val toolbar = fragment.requireView().findViewById<Toolbar>(R.id.gc_navigation_bar)

            toolbar.children.filterIsInstance<ImageButton>().first().performClick()

            verify(navController).navigate(NoResultsFragmentDirections.toCameraFragment())
        }
    }

    private fun launchNoResultsFragment(document: Document, block: (NoResultsFragment) -> Unit) {
        FragmentScenario.launchInContainer(
            fragmentClass = NoResultsFragment::class.java,
            fragmentArgs = Bundle().apply { putParcelable(ARGS_DOCUMENT, document) },
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                    NoResultsFragment().apply {
                        setListeners(enterManuallyButtonListener)
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                Navigation.setViewNavController(fragment.requireView(), navController)
                            }
                        }
                    }
            }
        ).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onFragment(block)
        }
    }

    private companion object {
        const val ARGS_DOCUMENT = "GC_ARGS_DOCUMENT"
    }
}
