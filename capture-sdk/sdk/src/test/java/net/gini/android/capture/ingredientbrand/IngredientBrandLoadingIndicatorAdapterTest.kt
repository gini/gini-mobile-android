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
 * The Camera and Analysis screens decide between the Gini mark and the integrator's indicator
 * every time the indicator is SHOWN, not when the screen is built — the client configuration may
 * not have arrived yet, the camera's four busy states are not all branded, and
 * [net.gini.android.capture.view.InjectedViewContainer] cannot re-inject a different adapter
 * afterwards.
 */
@RunWith(AndroidJUnit4::class)
class IngredientBrandLoadingIndicatorAdapterTest {

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
        IngredientBrandLoadingIndicatorAdapter({ enabled() }, { integrator })

    /** The child currently on screen — the host keeps both options and hides the unused one. */
    private fun hostChild(view: View) = (view as FrameLayout).children()
        .firstOrNull { it.visibility == View.VISIBLE }

    private fun FrameLayout.children() = (0 until childCount).map { getChildAt(it) }

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

    /**
     * Regression for the review finding on PR #993: the indicator was created once and reused, so
     * after a branded QR retrieval the Gini mark was still shown for a document import, which is
     * not a branded busy state.
     */
    @Test
    fun `swaps back to the integrator indicator when branding turns off between two shows`() {
        var brandedBusyState = true
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ brandedBusyState }, integrator)
        val view = sut.onCreateView(container)

        sut.onVisible()
        assertThat(hostChild(view)).isInstanceOf(ImageView::class.java)

        sut.onHidden()
        brandedBusyState = false
        sut.onVisible()

        assertThat(hostChild(view)).isNotInstanceOf(ImageView::class.java)
        assertThat(integrator.visible).isTrue()
    }

    /** The same in reverse: an unbranded busy state first must not latch out the Gini mark. */
    @Test
    fun `swaps to the Gini mark when branding turns on between two shows`() {
        var brandedBusyState = false
        val integrator = RecordingIntegratorAdapter()
        val sut = adapter({ brandedBusyState }, integrator)
        val view = sut.onCreateView(container)

        sut.onVisible()
        assertThat(integrator.visible).isTrue()

        sut.onHidden()
        brandedBusyState = true
        sut.onVisible()

        assertThat(hostChild(view)).isInstanceOf(ImageView::class.java)
        assertThat(integrator.visible).isFalse()
    }

    /**
     * The integrator may reuse one adapter across screens and
     * [net.gini.android.capture.view.InjectedViewAdapterInstance] tracks that ownership, so a swap
     * must never destroy it — only [IngredientBrandLoadingIndicatorAdapter.onDestroy] and
     * [IngredientBrandLoadingIndicatorAdapter.onCreateView] tear children down.
     */
    @Test
    fun `swapping away from the integrator indicator does not destroy it`() {
        var brandedBusyState = false
        var destroyed = false
        val integrator = object : CustomLoadingIndicatorAdapter {
            override fun onCreateView(container: ViewGroup) = View(container.context)
            override fun onVisible() = Unit
            override fun onHidden() = Unit
            override fun onDestroy() { destroyed = true }
        }
        val sut = adapter({ brandedBusyState }, integrator)
        sut.onCreateView(container)

        sut.onVisible()
        brandedBusyState = true
        sut.onVisible()

        assertThat(destroyed).isFalse()
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
