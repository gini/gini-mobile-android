package net.gini.android.capture.ingredientbrand

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter

/**
 * A loading indicator that picks between the Gini brand mark and the integrator's own indicator
 * **every time it is shown**, not when the screen is built.
 *
 * Neither screen can decide at view-creation time:
 *
 *  * The Camera screen is the first screen the SDK opens, and `ingredientBrandScreens` reaches
 *    [net.gini.android.capture.internal.provider.GiniBankConfigurationProvider] asynchronously
 *    (`/configurations` → DataStore → `GiniCaptureViewModel`). It also raises one indicator for
 *    four different busy states, and only the QR invoice retrieval carries the brand — so the
 *    answer changes within a single screen.
 *  * The Analysis screen is usually reached after the Camera screen, by which time the
 *    configuration has arrived — but not on the "open with" path, where
 *    `GiniCaptureFragment` navigates straight to Analysis and the provider is still empty on a
 *    cold first launch.
 *
 * Deciding once therefore latched the wrong indicator in both places, and re-binding a different
 * adapter afterwards does not help: [net.gini.android.capture.view.InjectedViewContainer] only
 * injects a view on a lifecycle `onStart`.
 *
 * So this adapter is bound unconditionally and hosts both options. Each is created the first time
 * it is actually needed and then kept, and [onVisible] shows whichever the current configuration
 * calls for. The integrator's indicator is never destroyed by a swap: the integrator may reuse one
 * adapter across screens and [net.gini.android.capture.view.InjectedViewAdapterInstance] tracks
 * that ownership, so only [onCreateView] and [onDestroy] tear children down.
 *
 * The integrator's indicator is created only when ingredient branding is off, so a bank still
 * cannot replace or suppress the Gini mark when it is on (PP-3512).
 */
internal class IngredientBrandLoadingIndicatorAdapter(
    private val isGiniMarkEnabled: () -> Boolean,
    private val integratorAdapter: () -> CustomLoadingIndicatorAdapter,
) : CustomLoadingIndicatorAdapter {

    private class Child(
        val adapter: CustomLoadingIndicatorAdapter,
        val view: View,
    )

    private var host: FrameLayout? = null
    private var giniChild: Child? = null
    private var integratorChild: Child? = null

    /** The child currently shown, so a repeated [onVisible] does not restart a running animation. */
    private var shown: Child? = null

    override fun onCreateView(container: ViewGroup): View {
        destroyChildren()
        // Sized by whichever indicator ends up inside it, so neither is constrained by the host.
        return FrameLayout(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }.also { host = it }
    }

    override fun onVisible() {
        val host = host ?: return
        val wanted = if (isGiniMarkEnabled()) giniChild(host) else integratorChild(host)
        if (shown !== wanted) {
            shown?.let {
                it.adapter.onHidden()
                it.view.visibility = View.GONE
            }
            shown = wanted
        }
        host.visibility = View.VISIBLE
        wanted.view.visibility = View.VISIBLE
        wanted.adapter.onVisible()
    }

    override fun onHidden() {
        shown?.adapter?.onHidden()
        host?.visibility = View.GONE
    }

    override fun onDestroy() {
        destroyChildren()
        host = null
    }

    private fun giniChild(host: FrameLayout): Child =
        giniChild ?: addChild(host, GiniLoadingIndicatorAdapter()).also { giniChild = it }

    private fun integratorChild(host: FrameLayout): Child =
        integratorChild ?: addChild(host, integratorAdapter()).also { integratorChild = it }

    private fun addChild(host: FrameLayout, adapter: CustomLoadingIndicatorAdapter): Child {
        val view = adapter.onCreateView(host)
        // The indicator may already be parented if the integrator reuses one adapter across
        // screens; the container would otherwise refuse to add it.
        (view.parent as? ViewGroup)?.removeView(view)
        view.visibility = View.GONE
        host.addView(view)
        return Child(adapter, view)
    }

    private fun destroyChildren() {
        // Only the Gini mark is ours to destroy. The integrator's adapter may be shared with
        // another screen, so it is only detached from this host.
        giniChild?.adapter?.onDestroy()
        giniChild = null
        integratorChild = null
        shown = null
        host?.removeAllViews()
    }
}
