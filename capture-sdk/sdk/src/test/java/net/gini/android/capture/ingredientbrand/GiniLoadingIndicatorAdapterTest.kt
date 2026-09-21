package net.gini.android.capture.ingredientbrand

import android.graphics.drawable.Animatable
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import org.junit.Test
import org.robolectric.Shadows
import org.junit.runner.RunWith

/**
 * The animated Gini loading indicator shown on the Analysis screen while the client configuration
 * lists that screen in `ingredientBrandScreens`.
 *
 * Whether the animation *looks* right is manual QA — Robolectric does not drive the frame clock.
 * These tests cover the adapter's contract with
 * [net.gini.android.capture.view.InjectedViewContainer], which is where the screen actually
 * broke once before.
 */
@RunWith(AndroidJUnit4::class)
class GiniLoadingIndicatorAdapterTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val container = FrameLayout(context)

    private class RecordingFallback : CustomLoadingIndicatorAdapter {
        var createdView = false
        override fun onCreateView(container: ViewGroup): View {
            createdView = true
            return View(container.context)
        }

        override fun onVisible() = Unit
        override fun onHidden() = Unit
        override fun onDestroy() = Unit
    }

    @Test
    fun `onCreateView returns the animation view, which starts hidden`() {
        val adapter = GiniLoadingIndicatorAdapter()

        val view = adapter.onCreateView(container)

        assertThat(view).isInstanceOf(ImageView::class.java)
        assertThat(view.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun `onVisible shows the view`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container)

        adapter.onVisible()

        assertThat(view.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun `onHidden hides the view`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container)
        adapter.onVisible()

        adapter.onHidden()

        assertThat(view.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun `the animation view is excluded from accessibility`() {
        val adapter = GiniLoadingIndicatorAdapter()

        val view = adapter.onCreateView(container)

        // The analysis message already announces the state; the mark must not add a second
        // TalkBack focus stop that says nothing.
        assertThat(view.importantForAccessibility)
            .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
    }


    /**
     * InjectedViewContainer calls onDestroy() immediately before onCreateView() on every
     * injection, so the adapter has to build a usable animation each time rather than hand back
     * one it prepared earlier. Getting this wrong showed a blank screen on device while every
     * other test still passed.
     */
    @Test
    fun `survives the destroy-then-create cycle the container performs on every injection`() {
        val adapter = GiniLoadingIndicatorAdapter()

        adapter.onDestroy()
        val view = adapter.onCreateView(container)
        adapter.onVisible()

        assertThat(view).isInstanceOf(ImageView::class.java)
        assertThat(view.visibility).isEqualTo(View.VISIBLE)
        assertThat((view as ImageView).drawable).isNotNull()
    }

    @Test
    fun `re-creating the view hands back a fresh, drawable-backed view`() {
        val adapter = GiniLoadingIndicatorAdapter()

        val first = adapter.onCreateView(container)
        val second = adapter.onCreateView(container)

        assertThat(second).isNotSameInstanceAs(first)
        assertThat((second as ImageView).drawable).isNotNull()
    }

    @Test
    fun `onDestroy detaches the drawable from the view`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container)

        adapter.onDestroy()

        assertThat((view as ImageView).drawable).isNull()
    }

    @Test
    fun `does not touch the fallback while the animation resolves`() {
        val fallback = RecordingFallback()
        val adapter = GiniLoadingIndicatorAdapter(fallback)

        val view = adapter.onCreateView(container)

        // The fallback exists only for a missing or unreadable drawable. While the animation
        // resolves — which is every normal run — it must not be created, or the screen would be
        // paying for two indicators.
        assertThat(view).isInstanceOf(ImageView::class.java)
        assertThat(fallback.createdView).isFalse()
    }

    /**
     * `stop()` ends the drawable's AnimatorSet, which fires the same `onAnimationEnd` the
     * loop-restart callback listens for. Without a guard, hiding the indicator restarts it and it
     * runs invisibly for the rest of the screen's life.
     */
    @Test
    fun `onHidden leaves the animation stopped`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container) as ImageView
        adapter.onVisible()

        adapter.onHidden()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat((view.drawable as Animatable).isRunning).isFalse()
    }

    @Test
    fun `onDestroy leaves nothing running`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container) as ImageView
        val drawable = view.drawable as Animatable
        adapter.onVisible()

        adapter.onDestroy()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(drawable.isRunning).isFalse()
    }

    /**
     * With system animations switched off every cycle would end the instant it began, so the
     * restart callback would spin. The drawable's initial state is the mark fully filled, so
     * leaving it unstarted still shows the complete brand mark.
     */
    @Test
    fun `does not animate when the user has switched system animations off`() {
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container) as ImageView

        adapter.onVisible()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(view.visibility).isEqualTo(View.VISIBLE)
        assertThat((view.drawable as Animatable).isRunning).isFalse()
    }

    /**
     * No explicit size anywhere: the drawable declares 119dp x 119dp and ImageView measures
     * itself from that, exactly as the default ProgressBar indicator does. That also means the
     * view honours the space the container offers instead of overriding it.
     */
    @Test
    fun `the animation view sizes itself from the drawable`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container) as ImageView
        val intrinsic = view.drawable.intrinsicWidth

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        assertThat(intrinsic).isGreaterThan(0)
        assertThat(view.measuredWidth).isEqualTo(intrinsic)
        assertThat(view.measuredHeight).isEqualTo(view.drawable.intrinsicHeight)
    }

    @Test
    fun `the animation view never exceeds the space offered to it`() {
        val adapter = GiniLoadingIndicatorAdapter()
        val view = adapter.onCreateView(container) as ImageView
        val cramped = view.drawable.intrinsicWidth / 2

        view.measure(
            View.MeasureSpec.makeMeasureSpec(cramped, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(cramped, View.MeasureSpec.AT_MOST),
        )

        assertThat(view.measuredWidth).isAtMost(cramped)
        assertThat(view.measuredHeight).isAtMost(cramped)
    }
}
