package net.gini.android.bank.sdk.capture.digitalinvoice.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.databinding.GbsFragmentDigitalInvoiceHelpBinding
import net.gini.android.bank.sdk.util.autoCleared
import net.gini.android.bank.sdk.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType

/**
 * Internal use only.
 */
class DigitalInvoiceHelpFragment : Fragment() {
    private var binding: GbsFragmentDigitalInvoiceHelpBinding by autoCleared()

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = GbsFragmentDigitalInvoiceHelpBinding.inflate(inflater)
        setupHelpList()
        setupTopBarNavigation()
        setupBottomNavigationBar()
        return binding.root
    }

    private fun setupHelpList() {
        val recyclerView = binding.gbsHelpItems
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = HelpItemAdapter(requireContext())
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer = binding.gbsInjectedNavigationBarContainerTop
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
            ) { injectedAdapterView ->
                val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                    NavButtonType.NONE else NavButtonType.BACK
                injectedAdapterView.setNavButtonType(navType)

                injectedAdapterView.setTitle(getString(net.gini.android.capture.R.string.gc_title_help))

                injectedAdapterView.setOnNavButtonClickListener(IntervalClickListener {
                    NavHostFragment.findNavController(this).popBackStack()
                })
            }
        }
    }

    private fun setupBottomNavigationBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            val injectedViewContainer = binding.gbsInjectedNavigationBarContainerBottom
            injectedViewContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(GiniBank.digitalInvoiceHelpNavigationBarBottomAdapterInstance) { injectedViewAdapter ->
                    injectedViewAdapter.setOnBackButtonClickListener(IntervalClickListener {
                        NavHostFragment.findNavController(this).popBackStack()
                    })
                }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = DigitalInvoiceHelpFragment()
    }
}