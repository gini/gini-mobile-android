package net.gini.android.capture.review.zoom

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.R
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real [ZoomInPreviewFragment] under Robolectric and checks that both ways of closing
 * it lead back to the previous screen.
 */
@RunWith(AndroidJUnit4::class)
class ZoomInPreviewFragmentTest {

    private lateinit var navController: NavController

    @Before
    fun setUp() {
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)
        navController = mock()
    }

    @Test
    fun `goes back when the close button is clicked`() {
        launchZoomInPreviewFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_action_close).performClick()

            verify(navController).popBackStack()
        }
    }

    @Test
    fun `goes back when back is pressed`() {
        launchZoomInPreviewFragment { fragment ->
            fragment.requireActivity().onBackPressedDispatcher.onBackPressed()

            verify(navController).popBackStack()
        }
    }

    private fun launchZoomInPreviewFragment(block: (ZoomInPreviewFragment) -> Unit) {
        FragmentScenario.launchInContainer(
            fragmentClass = ZoomInPreviewFragment::class.java,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                    ZoomInPreviewFragment.newInstance(ImageDocumentFake()).also { fragment ->
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
}
