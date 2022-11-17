package net.gini.android.capture.review.multipage.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.databinding.GcReviewNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter

interface ReviewNavigationBarBottomAdapter : InjectedViewAdapter {

    fun setOnContinueButtonClickListener(clickListener: View.OnClickListener)

    fun setOnAddPageButtonClickListener(clickListener: View.OnClickListener)

    fun setAddPageButtonVisibility(visibility: Int)

    fun setContinueButtonEnabled(enabled: Boolean)

    fun showLoadingIndicator()

    fun hideLoadingIndicator()
}

class DefaultReviewNavigationBarBottomAdapter : ReviewNavigationBarBottomAdapter {

    private var viewBinding: GcReviewNavigationBarBottomBinding? = null
    private var customLoadingIndicatorAdapter: OnButtonLoadingIndicatorAdapter? = null

    override fun setOnContinueButtonClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcContinue?.setOnClickListener(clickListener)
    }

    override fun setOnAddPageButtonClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcAddPage?.setOnClickListener(clickListener)
    }

    override fun setAddPageButtonVisibility(visibility: Int) {
        this.viewBinding?.gcAddPage?.visibility = visibility
    }

    override fun setContinueButtonEnabled(enabled: Boolean) {
        viewBinding?.gcContinue?.isEnabled = enabled
    }

    override fun hideLoadingIndicator() {
        this.customLoadingIndicatorAdapter?.onHidden()
    }

    override fun showLoadingIndicator() {
        this.customLoadingIndicatorAdapter?.onVisible()
    }


    override fun onCreateView(container: ViewGroup): View {
        val viewBinding =
            GcReviewNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context))

        this.viewBinding = viewBinding

        if (GiniCapture.hasInstance()) {
            this.customLoadingIndicatorAdapter = GiniCapture.getInstance().onButtonLoadingIndicatorAdapter
            viewBinding.gcInjectedLoadingIndicatorContainer.injectedViewAdapter = this.customLoadingIndicatorAdapter
        }

        return viewBinding.root
    }

    override fun onDestroy() {
        this.viewBinding = null
    }
}