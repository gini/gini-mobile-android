package net.gini.android.capture.noresults.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcNoResultsNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface NoResultsNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setOnBackButtonClickListener(click: View.OnClickListener)

}

class DefaultNoResultsNavigationBarBottomAdapter: NoResultsNavigationBarBottomAdapter {
    var viewBinding: GcNoResultsNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gcGoBack?.setOnClickListener(click)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GcNoResultsNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
