package net.gini.android.capture.noresults.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcNoResultsNavigationBarBottomBinding
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and pass it to the {@link GiniCapture.Builder#setNoResultsNavigationBarBottomAdapter(NoResultsNavigationBarBottomAdapter)}.
 */
interface NoResultsNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setOnBackButtonClickListener(click: View.OnClickListener?)

}

/**
 * Internal use only.
 */
class DefaultNoResultsNavigationBarBottomAdapter: NoResultsNavigationBarBottomAdapter {
    var viewBinding: GcNoResultsNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(click: View.OnClickListener?) {
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
