package net.gini.android.capture.error

import android.os.Bundle
import android.view.View
import android.widget.TextView
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
import net.gini.android.capture.EnterManuallyButtonListener
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module

/**
 * Runs the real [ErrorFragment] under Robolectric and checks that it shows the error it was
 * given and where its buttons lead.
 */
@RunWith(AndroidJUnit4::class)
class ErrorFragmentTest {

    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    private lateinit var navController: NavController
    private lateinit var enterManuallyButtonListener: EnterManuallyButtonListener

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
    fun `shows the title and description of the error type`() {
        launchErrorFragment(errorArgs(ErrorType.GENERAL)) { fragment ->
            val view = fragment.requireView()

            assertThat(view.findViewById<TextView>(R.id.gc_error_header).text.toString())
                .isEqualTo(fragment.getString(ErrorType.GENERAL.titleTextResource))
            assertThat(view.findViewById<TextView>(R.id.gc_error_textview).text.toString())
                .isEqualTo(fragment.getString(ErrorType.GENERAL.descriptionTextResource))
        }
    }

    @Test
    fun `shows the custom error as title`() {
        val args = Bundle().apply { putString(ARGS_CUSTOM_ERROR, CUSTOM_ERROR) }

        launchErrorFragment(args) { fragment ->
            assertThat(fragment.requireView().findViewById<TextView>(R.id.gc_error_header).text.toString())
                .isEqualTo(CUSTOM_ERROR)
        }
    }

    @Test
    fun `reports the enter manually button click to the listener`() {
        launchErrorFragment(errorArgs(ErrorType.GENERAL)) { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_button_error_enter_manually).performClick()

            verify(enterManuallyButtonListener).onEnterManuallyPressed()
        }
    }

    @Test
    fun `goes back to the camera when the retake images button is clicked`() {
        launchErrorFragment(errorArgs(ErrorType.GENERAL)) { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_button_error_retake_images).performClick()

            verify(navController).navigate(ErrorFragmentDirections.toCameraFragment())
        }
    }

    private fun errorArgs(errorType: ErrorType) = Bundle().apply {
        putSerializable(ARGS_ERROR, errorType)
        putParcelable(ARGS_DOCUMENT, ImageDocumentFake())
    }

    private fun launchErrorFragment(args: Bundle, block: (ErrorFragment) -> Unit) {
        FragmentScenario.launchInContainer(
            fragmentClass = ErrorFragment::class.java,
            fragmentArgs = args,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                    ErrorFragment().apply {
                        setListener(enterManuallyButtonListener)
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
        const val ARGS_ERROR = "GC_ARGS_ERROR"
        const val ARGS_DOCUMENT = "ARGS_DOCUMENT"
        const val ARGS_CUSTOM_ERROR = "ARGS_CUSTOM_ERROR"
        const val CUSTOM_ERROR = "Something custom went wrong"
    }
}
