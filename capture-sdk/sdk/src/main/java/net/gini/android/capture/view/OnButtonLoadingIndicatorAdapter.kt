package net.gini.android.capture.view

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import net.gini.android.capture.R

/**
 * Adapter for injecting a custom loading indicator which will be shown on buttons.
 */
interface OnButtonLoadingIndicatorAdapter : InjectedViewAdapter {

    /**
     * Called when the loading indicator needs to be shown.
     */
    fun onVisible()

    /**
     * Called when the loading indicator needs to be hidden.
     */
    fun onHidden()
}

class DefaultOnButtonLoadingIndicatorAdapter: OnButtonLoadingIndicatorAdapter {

    private lateinit var progressBar: ProgressBar

    override fun onVisible() {
        progressBar.visibility = View.VISIBLE
    }

    override fun onHidden() {
        progressBar.visibility = View.GONE
    }

    override fun onCreateView(container: ViewGroup): View {
        progressBar = ProgressBar(container.context).apply {
            layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            indeterminateTintMode = PorterDuff.Mode.SRC_IN
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Accent_01))
        }

        return progressBar
    }

    override fun onDestroy() {}
}