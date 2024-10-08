package net.gini.android.capture.review.multipage.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.databinding.GcReviewNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter

/**
 * Adapter for injecting a custom bottom navigation bar on the review screen.
 */
interface ReviewNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the continue button.
     *
     * @param clickListener the click listener for the button
     */
    fun setOnContinueButtonClickListener(clickListener: View.OnClickListener?)

    /**
     * Set the click listener for the "add page" button.
     *
     * @param clickListener the click listener for the button
     */
    fun setOnAddPageButtonClickListener(clickListener: View.OnClickListener?)

    /**
     * Set "add page" button visibility.
     *
     * @param visibility one of the view visibility values: [View.VISIBLE], [View.INVISIBLE], or [View.GONE]
     */
    fun setAddPageButtonVisibility(visibility: Int)

    /**
     * Set the enabled state of the continue button.
     *
     * @param clickListener the click listener for the button
     */
    fun setContinueButtonEnabled(enabled: Boolean)

    /**
     * Called when the loading indicator needs to be shown.
     */
    fun showLoadingIndicator()

    /**
     * Called when the loading indicator needs to be hidden.
     */
    fun hideLoadingIndicator()
}

class DefaultReviewNavigationBarBottomAdapter : ReviewNavigationBarBottomAdapter {

    private var viewBinding: GcReviewNavigationBarBottomBinding? = null

    override fun setOnContinueButtonClickListener(clickListener: View.OnClickListener?) {
        this.viewBinding?.gcContinue?.setOnClickListener(clickListener)
    }

    override fun setOnAddPageButtonClickListener(clickListener: View.OnClickListener?) {
        this.viewBinding?.gcAddPageButton?.setOnClickListener(clickListener)
    }

    override fun setAddPageButtonVisibility(visibility: Int) {
        this.viewBinding?.gcAddPagesWrapper?.visibility = visibility
        this.viewBinding?.gcAddPageButton?.visibility = visibility
    }

    override fun setContinueButtonEnabled(enabled: Boolean) {
        viewBinding?.gcContinue?.isEnabled = enabled
    }

    override fun hideLoadingIndicator() {
        viewBinding?.gcInjectedLoadingIndicatorContainer?.modifyAdapterIfOwned {
            (it as OnButtonLoadingIndicatorAdapter).onHidden()
        }
    }

    override fun showLoadingIndicator() {
        viewBinding?.gcInjectedLoadingIndicatorContainer?.modifyAdapterIfOwned {
            (it as OnButtonLoadingIndicatorAdapter).onVisible()
        }
    }


    override fun onCreateView(container: ViewGroup): View {
        val viewBinding =
            GcReviewNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context))

        this.viewBinding = viewBinding

        if (GiniCapture.hasInstance()) {
            viewBinding.gcInjectedLoadingIndicatorContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().onButtonLoadingIndicatorAdapterInstance
            ) {}
        }

        return viewBinding.root
    }

    override fun onDestroy() {
        this.viewBinding = null
    }
}