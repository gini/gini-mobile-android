package net.gini.android.capture.noresults.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcNoResultsNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and pass it to the {@link GiniCapture.Builder#setNoResultsNavigationBarBottomAdapter(NoResultsNavigationBarBottomAdapter)}.
 */
interface NoResultsNavigationBarBottomAdapter: InjectedViewAdapter {

    /**
     * Set the click listener for the back button.
     *
     * @param listener the click listener for the button
     */
    fun setOnBackButtonClickListener(listener: View.OnClickListener?)

}

/**
 * Internal use only.
 */
class DefaultNoResultsNavigationBarBottomAdapter: NoResultsNavigationBarBottomAdapter {
    var viewBinding: GcNoResultsNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gcGoBack?.setOnClickListener(listener)
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
