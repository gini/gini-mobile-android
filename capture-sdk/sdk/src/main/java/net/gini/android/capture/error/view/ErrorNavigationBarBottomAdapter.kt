package net.gini.android.capture.error.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcErrorNavigationBarBottomBinding
import net.gini.android.capture.databinding.GcNoResultsNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and pass it to the {@link GiniCapture.Builder#setErrorNavigationBarBottomAdapter(ErrorNavigationBarBottomAdapter)}.
 */
interface ErrorNavigationBarBottomAdapter: InjectedViewAdapter {
    fun setOnBackButtonClickListener(click: View.OnClickListener)
}

/**
 * Internal use only.
 */
class DefaultErrorNavigationBarBottomAdapter: ErrorNavigationBarBottomAdapter {
    var viewBinding: GcErrorNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gcGoBack?.setOnClickListener(click)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GcErrorNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
