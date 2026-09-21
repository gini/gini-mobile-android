package net.gini.android.capture.ingredientbrand

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Camera screen decides between the Gini mark and the integrator's indicator when the
 * indicator is SHOWN, not when the screen is built — on launch the client configuration has not
 * arrived yet, and [net.gini.android.capture.view.InjectedViewContainer] cannot re-inject a
 * different adapter afterwards.
 */
@RunWith(AndroidJUnit4::class)
class CameraLoadingIndicatorAdapterTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val container = FrameLayout(context)

    private class RecordingIntegratorAdapter : CustomLoadingIndicatorAdapter {
        var created = false
        var visible = false
        override fun onCreateView(container: ViewGroup): View {
            created = true
            return View(container.context)
        }
        override fun onVisible() { visible = true }
        override fun onHidden() { visible = false }
        override fun onDestroy() = Unit
    }

    private fun adapter(enabled: () -> Boolean, integrator: CustomLoadingIndicatorAdapter) =
        CameraLoadingIndicatorAdapter({ enabled() }, { integrator })

    private fun hostChild(view: View) =
        (view as FrameLayout).let { if (it.childCount > 0) it.getChildAt(0) else null }

    /**
     * The whole point: the flag is still empty at onCreateView and only becomes true later. The
     * adapter must pick up the late value.
     */
    @Test
    fun `uses the Gini mark when the flag arrives after the view was created`() {
        var enabled = false
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ enabled }, integrator)

        val view = sut.onCreateView(container)   // flag not known yet
        enabled = true                            // configuration lands
        sut.onVisible()

        assertThat(hostChild(view)).isInstanceOf(ImageView::class.java)
        assertThat(integrator.created).isFalse()
    }

    @Test
    fun `uses the integrator indicator when ingredient branding is off`() {
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ false }, integrator)

        val view = sut.onCreateView(container)
        sut.onVisible()

        assertThat(integrator.created).isTrue()
        assertThat(integrator.visible).isTrue()
        assertThat(hostChild(view)).isNotInstanceOf(ImageView::class.java)
    }

    @Test
    fun `does not create the integrator indicator while ingredient branding is on`() {
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ true }, integrator)

        sut.onCreateView(container)
        sut.onVisible()

        // A bank must not be able to replace or suppress the Gini mark.
        assertThat(integrator.created).isFalse()
    }

    @Test
    fun `the host starts hidden and toggles with visibility`() {
        val sut = adapter({ true }, RecordingIntegratorAdapter())
        val view = sut.onCreateView(container)

        sut.onVisible()
        assertThat(view.visibility).isEqualTo(View.VISIBLE)

        sut.onHidden()
        assertThat(view.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun `re-creating the view clears the previous indicator`() {
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ false }, integrator)
        sut.onCreateView(container)
        sut.onVisible()

        val second = sut.onCreateView(container) as FrameLayout

        assertThat(second.childCount).isEqualTo(0)
    }

    @Test
    fun `onDestroy empties the host`() {
        val sut = adapter({ true }, RecordingIntegratorAdapter())
        val view = sut.onCreateView(container) as FrameLayout
        sut.onVisible()

        sut.onDestroy()

        assertThat(view.childCount).isEqualTo(0)
    }

    /**
     * The camera raises one indicator for four busy states — QR invoice retrieval, the client
     * document check, a multi-file import and a freshly taken photo. Only the first is the
     * analysis step of the QR flow, so only that one carries the ingredient brand even when the
     * client has branding switched on.
     */
    @Test
    fun `uses the integrator indicator for a non-QR busy state even when branding is on`() {
        var qrRetrievalRunning = false
        val brandingOn = true
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ qrRetrievalRunning && brandingOn }, integrator)

        sut.onCreateView(container)
        sut.onVisible()

        assertThat(integrator.created).isTrue()
    }

    @Test
    fun `uses the Gini mark once the QR busy state is the one running`() {
        var qrRetrievalRunning = false
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ qrRetrievalRunning }, integrator)

        qrRetrievalRunning = true
        val view = sut.onCreateView(container)
        sut.onVisible()

        assertThat(hostChild(view)).isInstanceOf(ImageView::class.java)
        assertThat(integrator.created).isFalse()
    }
}
