package net.gini.android.capture.ingredientbrand

import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import net.gini.android.capture.R
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter

/**
 * Shows the animated Gini brand mark in place of the loading indicator.
 *
 * Bound by the Analysis screen instead of the integrator's adapter while the client
 * configuration lists that screen in `ingredientBrandScreens`. This is deliberately not a
 * customisation point: it is never exposed through `GiniCapture.Builder`, and while ingredient
 * branding is on, an adapter set with `GiniCapture.Builder.setLoadingIndicatorAdapter(…)` is not
 * consulted on the Analysis screen (see PP-3512).
 *
 * The animation is an [AnimatedVectorDrawableCompat], so it renders identically on every API
 * level this module supports and needs no image-decoding dependency —
 * `androidx.vectordrawable-animated` already arrives with `androidx.appcompat`.
 *
 * The drawable is created in [onCreateView] and dropped in [onDestroy], never held ready in
 * advance: [net.gini.android.capture.view.InjectedViewContainer] calls `onDestroy()` immediately
 * before `onCreateView()` on every injection, so an adapter that handed back a prepared view
 * would return one it had just torn down. `DefaultLoadingIndicatorAdapter` follows the same
 * create-fresh model.
 */
internal class GiniLoadingIndicatorAdapter(
    private val fallback: CustomLoadingIndicatorAdapter = DefaultLoadingIndicatorAdapter(),
) : CustomLoadingIndicatorAdapter {

    private var animationView: ImageView? = null
    private var animation: AnimatedVectorDrawableCompat? = null
    private var usingFallback: Boolean = false

    /**
     * Whether the indicator is meant to be playing right now.
     *
     * `stop()` on an [AnimatedVectorDrawableCompat] ends its underlying `AnimatorSet`, which
     * fires the same `onAnimationEnd` callback as reaching the end of a cycle. Without this flag
     * the restart below would fight [onHidden]: the animation would keep running invisibly for
     * the rest of the screen's life.
     */
    private var running: Boolean = false

    override fun onCreateView(container: ViewGroup): View {
        releaseAnimation()

        val drawable = AnimatedVectorDrawableCompat.create(
            container.context,
            R.drawable.gc_analysis_gini_loading,
        )
        if (drawable == null) {
            // Nothing to animate — degrade the branding, never the analysis flow.
            usingFallback = true
            return fallback.onCreateView(container)
        }

        // The animation is authored to loop, but repeatCount on an objectAnimator inside an
        // AnimatedVectorDrawableCompat is not honoured on every API level below 24. Restarting on
        // end covers those; where the loop does work the callback simply never fires.
        drawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                if (!running) return
                animationView?.post { if (running) animation?.start() }
            }
        })

        usingFallback = false
        animation = drawable
        return ImageView(container.context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            // Announced by TalkBack like the iOS indicator. The text is a neutral "Loading" because
            // this adapter also runs on the camera screen's QR code overlay, not only on Analysis.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = container.context.getString(
                R.string.gc_gini_loading_indicator_content_description,
            )
            // Matches DefaultLoadingIndicatorAdapter, which also starts hidden and is made visible
            // by the screen's onVisible() call.
            visibility = View.GONE
            // No explicit size: the drawable declares 119dp x 119dp, so ImageView measures itself
            // from it and still honours the space the container offers — same as the default
            // ProgressBar indicator, which also carries no size of its own.
            setImageDrawable(drawable)
        }.also { animationView = it }
    }

    override fun onVisible() {
        if (usingFallback) {
            fallback.onVisible()
            return
        }
        val view = animationView ?: return
        view.visibility = View.VISIBLE
        // When animations are switched off system-wide the drawable is left unstarted: its
        // initial state is the mark fully filled, so the complete brand mark still shows, and a
        // restart loop is avoided since every cycle would otherwise end the instant it began.
        if (motionEnabled(view.context)) {
            running = true
            animation?.start()
        }
    }

    override fun onHidden() {
        if (usingFallback) {
            fallback.onHidden()
            return
        }
        // Stopped before hiding, so the animation never leaves a half-drawn frame behind for the
        // next time the indicator is shown.
        running = false
        animation?.stop()
        animationView?.visibility = View.GONE
    }

    override fun onDestroy() {
        releaseAnimation()
        fallback.onDestroy()
    }

    /**
     * Whether the user has system animations switched on.
     *
     * Read per call rather than cached: the setting can change while the app is backgrounded.
     */
    private fun motionEnabled(context: Context): Boolean = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) != 0f

    private fun releaseAnimation() {
        running = false
        animation?.let { drawable ->
            drawable.clearAnimationCallbacks()
            drawable.stop()
        }
        animation = null
        animationView?.setImageDrawable(null)
        animationView = null
    }
}
