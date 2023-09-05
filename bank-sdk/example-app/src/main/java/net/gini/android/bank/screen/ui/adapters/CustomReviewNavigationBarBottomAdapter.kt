package net.gini.android.bank.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.screen.databinding.CustomReviewNavigationBarBottomBinding
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.review.multipage.view.ReviewNavigationBarBottomAdapter
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter

class CustomReviewNavigationBarBottomAdapter : ReviewNavigationBarBottomAdapter {

    private var viewBinding: CustomReviewNavigationBarBottomBinding? = null

    override fun setOnContinueButtonClickListener(clickListener: View.OnClickListener?) {
        this.viewBinding?.buttonContinue?.setOnClickListener(clickListener)
    }

    override fun setOnAddPageButtonClickListener(clickListener: View.OnClickListener?) {
        this.viewBinding?.buttonAddPageButton?.setOnClickListener(clickListener)
    }

    override fun setAddPageButtonVisibility(visibility: Int) {
        this.viewBinding?.linearLayoutAddPagesWrapper?.visibility = visibility
        this.viewBinding?.buttonAddPageButton?.visibility = visibility
    }

    override fun setContinueButtonEnabled(enabled: Boolean) {
        viewBinding?.buttonContinue?.isEnabled = enabled
    }

    override fun hideLoadingIndicator() {
        viewBinding?.injectedViewContainerInjectedLoadingIndicatorContainer?.modifyAdapterIfOwned {
            (it as OnButtonLoadingIndicatorAdapter).onHidden()
        }
    }

    override fun showLoadingIndicator() {
        viewBinding?.injectedViewContainerInjectedLoadingIndicatorContainer?.modifyAdapterIfOwned {
            (it as OnButtonLoadingIndicatorAdapter).onVisible()
        }
    }


    override fun onCreateView(container: ViewGroup): View {
        val viewBinding =
            CustomReviewNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context))

        this.viewBinding = viewBinding

        if (GiniCapture.hasInstance()) {
            viewBinding.injectedViewContainerInjectedLoadingIndicatorContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().onButtonLoadingIndicatorAdapterInstance
            ) {}
        }

        return viewBinding.root
    }

    override fun onDestroy() {
        this.viewBinding = null
    }
}