package net.gini.android.capture.review.multipage.view

import android.content.DialogInterface.OnClickListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcReviewNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface ReviewNavigationBarBottomAdapter: InjectedViewAdapter {

    fun onContinueClickListener(clickListener: View.OnClickListener)

    fun onAddPageClickListener(clickListener: View.OnClickListener)

    fun onAddPageVisible(visibility: Int)
}

class DefaultReviewNavigationBarBottomAdapter: ReviewNavigationBarBottomAdapter {

    private var viewBinding: GcReviewNavigationBarBottomBinding? = null

    override fun onContinueClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcContinue?.setOnClickListener(clickListener)
    }

    override fun onAddPageClickListener(clickListener: View.OnClickListener) {
        this.viewBinding?.gcAddPage?.setOnClickListener(clickListener)
    }

    override fun onAddPageVisible(visibility: Int) {
        this.viewBinding?.gcAddPage?.visibility = visibility
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