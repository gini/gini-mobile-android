package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import net.gini.android.capture.R
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter

class CustomOnButtonLoadingIndicatorAdapter: OnButtonLoadingIndicatorAdapter {

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
            indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gc_error_01))
        }

        this.progressBar = progressBar


        return progressBar
    }

    override fun onDestroy() {}
}