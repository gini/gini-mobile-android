package net.gini.android.capture.error.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcErrorNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter


/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and pass it to the {@link GiniCapture.Builder#setErrorNavigationBarBottomAdapter(ErrorNavigationBarBottomAdapter)}
 */
interface ErrorNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the back button.
     *
     * @param listener the click listener for the button
     */
    fun setOnBackClickListener(listener: View.OnClickListener?)
}

/**
 * Internal use only.
 */
internal class DefaultErrorNavigationBarBottomAdapter : ErrorNavigationBarBottomAdapter {

    var binding: GcErrorNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(listener: View.OnClickListener?) {
        binding?.gcGoBack?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {

        val viewBinding = GcErrorNavigationBarBottomBinding.inflate(
            LayoutInflater.from(container.context), container, false
        )
        binding = viewBinding

        return viewBinding.root
    }

    override fun onDestroy() {
        binding = null
    }
}
