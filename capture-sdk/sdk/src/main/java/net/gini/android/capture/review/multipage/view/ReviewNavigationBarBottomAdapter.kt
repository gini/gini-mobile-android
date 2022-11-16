package net.gini.android.capture.review.multipage.view

import android.content.DialogInterface.OnClickListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcReviewNavigationBarBottomBinding
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewAdapter

interface ReviewNavigationBarBottomAdapter: InjectedViewAdapter {

    fun onContinueClickListener(clickListener: View.OnClickListener)

    fun onAddPageClickListener(clickListener: View.OnClickListener)

    fun onAddPageVisible(visibility: Int)

    fun onButtonStatus(enabled: Boolean)

    fun onLoadingIndicatorSet(customLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter?)

    fun onLoadingIndicatorGet(): CustomLoadingIndicatorAdapter?
}

class DefaultReviewNavigationBarBottomAdapter: ReviewNavigationBarBottomAdapter {

    private var viewBinding: GcReviewNavigationBarBottomBinding? = null
    private var customLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter? = null

    override fun onContinueClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcContinue?.setOnClickListener(clickListener)
    }

    override fun onAddPageClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcAddPage?.setOnClickListener(clickListener)
    }

    override fun onAddPageVisible(visibility: Int) {
        this.viewBinding?.gcAddPage?.visibility = visibility
    }

    override fun onButtonStatus(enabled: Boolean) {
        viewBinding?.gcContinue?.isEnabled = enabled
    }

    override fun onLoadingIndicatorSet(customLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter?) {

        this@DefaultReviewNavigationBarBottomAdapter.customLoadingIndicatorAdapter =
            customLoadingIndicatorAdapter ?: DefaultLoadingIndicatorAdapter()

        this@DefaultReviewNavigationBarBottomAdapter.viewBinding?.gcInjectedLoadingIndicatorContainer?.injectedViewAdapter =
            this@DefaultReviewNavigationBarBottomAdapter.customLoadingIndicatorAdapter

    }

    override fun onLoadingIndicatorGet(): CustomLoadingIndicatorAdapter? {
        return this.customLoadingIndicatorAdapter
    }



    override fun onCreateView(container: ViewGroup): View {
        val viewBinding = GcReviewNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context))

        this.viewBinding = viewBinding

        return viewBinding.root
    }

    override fun onDestroy() {
        this.viewBinding = null
    }
}