package net.gini.android.capture

import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.analysis.AnalysisFragment
import net.gini.android.capture.camera.CameraFragment
import net.gini.android.capture.error.ErrorFragment
import net.gini.android.capture.noresults.NoResultsFragment
import net.gini.android.capture.review.multipage.MultiPageReviewFragment
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureFragmentFactoryTest {

    private val factory = CaptureFragmentFactory(
        cameraListener = mockk(relaxed = true),
        analysisFragmentListener = mockk(relaxed = true),
        bankSDKBridge = null,
        enterManuallyButtonListener = mockk(relaxed = true),
        cancelListener = mockk(relaxed = true),
    )

    @Test
    fun `creates the camera fragment`() {
        assertThat(instantiate(CameraFragment::class.java)).isInstanceOf(CameraFragment::class.java)
    }

    @Test
    fun `creates the analysis fragment`() {
        assertThat(instantiate(AnalysisFragment::class.java)).isInstanceOf(AnalysisFragment::class.java)
    }

    @Test
    fun `creates the error fragment`() {
        assertThat(instantiate(ErrorFragment::class.java)).isInstanceOf(ErrorFragment::class.java)
    }

    @Test
    fun `creates the no results fragment`() {
        assertThat(instantiate(NoResultsFragment::class.java)).isInstanceOf(NoResultsFragment::class.java)
    }

    @Test
    fun `creates the multi-page review fragment`() {
        assertThat(instantiate(MultiPageReviewFragment::class.java))
            .isInstanceOf(MultiPageReviewFragment::class.java)
    }

    @Test
    fun `creates fragments it has no listeners for through the default factory`() {
        assertThat(instantiate(PlainFragment::class.java)).isInstanceOf(PlainFragment::class.java)
    }

    private fun instantiate(fragmentClass: Class<out Fragment>): Fragment =
        factory.instantiate(fragmentClass.classLoader!!, fragmentClass.name)

    class PlainFragment : Fragment()
}
