package net.gini.android.capture.view

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.runtimecolors.giniColor

/**
 * Adapter for injecting a custom progress/loading animation.
 */
interface CustomLoadingIndicatorAdapter : InjectedViewAdapter {
    /**
     * Called when the loading indicator is visible. If you use animations, then you can start the animation here.
     */
    fun onVisible()
    /**
     * Called when the loading indicator is hidden. If you use animations, then you can stop the animation here.
     */
    fun onHidden()
}

class DefaultLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter {

    private var progressBar: ProgressBar? = null

    override fun onVisible() {
        progressBar?.visibility = View.VISIBLE
    }

    override fun onHidden() {
        progressBar?.visibility = View.GONE
    }

    override fun onCreateView(container: ViewGroup): View {
        val progressBar = ProgressBar(container.context).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            indeterminateTintMode = PorterDuff.Mode.SRC_IN
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(context.giniColor(R.color.gc_accent_01))
            visibility = View.GONE
        }
        this.progressBar = progressBar

        return progressBar
    }

    override fun onDestroy() { /* No-op */ }
}
