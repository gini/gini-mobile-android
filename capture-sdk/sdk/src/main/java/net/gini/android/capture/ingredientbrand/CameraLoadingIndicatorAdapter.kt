package net.gini.android.capture.ingredientbrand

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter

/**
 * The Camera screen's loading indicator, which picks between the Gini brand mark and the
 * integrator's own indicator **when it is shown**, not when the screen is built.
 *
 * The Analysis screen can decide at view-creation time because by then the client configuration
 * has arrived. The Camera screen cannot: it is the first screen the SDK opens, and
 * `ingredientBrandScreens` reaches [net.gini.android.capture.internal.provider.GiniBankConfigurationProvider]
 * asynchronously (`/configurations` → DataStore → `GiniCaptureViewModel`). Deciding in
 * `onCreateView` therefore latched "no ingredient brand" on first launch and never re-evaluated,
 * because [net.gini.android.capture.view.InjectedViewContainer] only injects a view on a lifecycle
 * `onStart` — re-binding a different adapter afterwards has no effect until the screen restarts.
 *
 * So this adapter is bound unconditionally and hosts both options in a container, creating
 * whichever is needed the first time the indicator actually becomes visible. That is the moment
 * the camera shows while an invoice is retrieved for a scanned QR code — the analysis step of the
 * QR flow — and while an imported or freshly taken document is processed.
 *
 * The integrator's indicator is created only when ingredient branding is off, so a bank still
 * cannot replace or suppress the Gini mark when it is on (PP-3512).
 */
internal class CameraLoadingIndicatorAdapter(
    private val isGiniMarkEnabled: () -> Boolean,
    private val integratorAdapter: () -> CustomLoadingIndicatorAdapter,
) : CustomLoadingIndicatorAdapter {

    private var host: FrameLayout? = null
    private var active: CustomLoadingIndicatorAdapter? = null

    override fun onCreateView(container: ViewGroup): View {
        destroyActive()
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
        if (active == null) {
            val adapter =
                if (isGiniMarkEnabled()) GiniLoadingIndicatorAdapter() else integratorAdapter()
            val view = adapter.onCreateView(host)
            // The indicator may already be parented if the integrator reuses one adapter across
            // screens; the container would otherwise refuse to add it.
            (view.parent as? ViewGroup)?.removeView(view)
            host.addView(view)
            active = adapter
        }
        host.visibility = View.VISIBLE
        active?.onVisible()
    }

    override fun onHidden() {
        active?.onHidden()
        host?.visibility = View.GONE
    }

    override fun onDestroy() {
        destroyActive()
        host = null
    }

    private fun destroyActive() {
        active?.onDestroy()
        active = null
        host?.removeAllViews()
    }
}
