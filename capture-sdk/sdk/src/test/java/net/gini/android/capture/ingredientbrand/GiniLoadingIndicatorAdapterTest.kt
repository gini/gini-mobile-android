package net.gini.android.capture.ingredientbrand

import android.app.Activity
import android.graphics.drawable.Animatable
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import net.gini.android.capture.R
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import org.junit.After
import org.junit.Test
import org.robolectric.Robolectric
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
        var visibleCount = 0
        var hiddenCount = 0
        var destroyCount = 0

        override fun onCreateView(container: ViewGroup): View {
            createdView = true
            return View(container.context)
        }

        override fun onVisible() {
            visibleCount++
        }

        override fun onHidden() {
            hiddenCount++
        }

        override fun onDestroy() {
            destroyCount++
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(AnimatedVectorDrawableCompat::class)
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
    fun `the animation view is announced by TalkBack`() {
        val adapter = GiniLoadingIndicatorAdapter()

        val view = adapter.onCreateView(container)

        // Matches iOS, where VoiceOver announces the Gini loading indicator.
        assertThat(view.importantForAccessibility)
            .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
        assertThat(view.contentDescription.toString()).isEqualTo(
            context.getString(R.string.gc_gini_loading_indicator_content_description)
        )
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

    /**
     * `AnimatedVectorDrawableCompat.create` answers null when the drawable cannot be inflated.
     * The branding then degrades to the integrator's indicator — the analysis flow itself must
     * never be the thing that gives way.
     */
    @Test
    fun `falls back to the integrator indicator when the animation cannot be created`() {
        mockkStatic(AnimatedVectorDrawableCompat::class)
        every { AnimatedVectorDrawableCompat.create(any(), any()) } returns null
        val fallback = RecordingFallback()
        val adapter = GiniLoadingIndicatorAdapter(fallback)

        val view = adapter.onCreateView(container)
        adapter.onVisible()
        adapter.onHidden()
        adapter.onDestroy()

        assertThat(fallback.createdView).isTrue()
        assertThat(view).isNotInstanceOf(ImageView::class.java)
        assertThat(fallback.visibleCount).isEqualTo(1)
        assertThat(fallback.hiddenCount).isEqualTo(1)
        assertThat(fallback.destroyCount).isEqualTo(1)
    }

    /**
     * The drawable's own loop is not honoured on every API level below 24, so the adapter
     * restarts the animation when a cycle ends — but only while the indicator is still shown.
     */
    @Test
    fun `restarts the animation when a cycle ends while the indicator is shown`() {
        val (adapter, drawable, callback) = adapterWithCapturedAnimationCallback()
        adapter.onVisible()

        callback.onAnimationEnd(drawable)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 2) { drawable.start() }
    }

    /**
     * `stop()` fires the same `onAnimationEnd` as a finished cycle. Hiding the indicator must not
     * restart it, or it would keep running invisibly for the rest of the screen's life.
     */
    @Test
    fun `does not restart the animation when the cycle ends because the indicator was hidden`() {
        val (adapter, drawable, callback) = adapterWithCapturedAnimationCallback()
        adapter.onVisible()
        adapter.onHidden()

        callback.onAnimationEnd(drawable)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) { drawable.start() }
    }

    /** The restart is posted; if the indicator is hidden before the post runs, it must stay stopped. */
    @Test
    fun `drops a pending restart when the indicator is hidden before it runs`() {
        val (adapter, drawable, callback) = adapterWithCapturedAnimationCallback()
        adapter.onVisible()

        callback.onAnimationEnd(drawable)
        adapter.onHidden()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 1) { drawable.start() }
    }

    private data class CapturedAdapter(
        val adapter: GiniLoadingIndicatorAdapter,
        val drawable: AnimatedVectorDrawableCompat,
        val callback: Animatable2Compat.AnimationCallback,
    )

    /**
     * Robolectric does not drive the animation's frame clock, so the end-of-cycle callback never
     * fires on its own. A stand-in drawable hands the registered callback back to the test, which
     * then fires it exactly where a real cycle would.
     *
     * The view is attached to a window, as InjectedViewContainer would: the restart is delivered
     * with `View.post`, which a detached view only queues until it is attached.
     */
    private fun adapterWithCapturedAnimationCallback(): CapturedAdapter {
        val drawable = mockk<AnimatedVectorDrawableCompat>(relaxed = true)
        val callbackSlot = slot<Animatable2Compat.AnimationCallback>()
        every { drawable.registerAnimationCallback(capture(callbackSlot)) } just runs
        mockkStatic(AnimatedVectorDrawableCompat::class)
        every { AnimatedVectorDrawableCompat.create(any(), any()) } returns drawable
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val attachedContainer = FrameLayout(activity).also { activity.setContentView(it) }
        val adapter = GiniLoadingIndicatorAdapter()
        attachedContainer.addView(adapter.onCreateView(attachedContainer))
        return CapturedAdapter(adapter, drawable, callbackSlot.captured)
    }
}
