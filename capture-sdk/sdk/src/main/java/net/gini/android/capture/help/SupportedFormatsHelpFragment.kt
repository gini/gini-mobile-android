package net.gini.android.capture.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentSupportedFormatsHelpBinding
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 */
class SupportedFormatsHelpFragment : Fragment() {
    private var binding: GcFragmentSupportedFormatsHelpBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentSupportedFormatsHelpBinding.inflate(inflater)
        setUpFormatsList()
        setupBottomBarNavigation()
        setupTopBarNavigation()
        return binding.root
    }

    private fun setUpFormatsList() {
        val recyclerView: RecyclerView = binding.gcFormatsList
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = SupportedFormatsAdapter(false)
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer = binding.gcInjectedNavigationBarContainerTop
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder<NavigationBarTopAdapter>(
                GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
            ) { injectedViewAdapter: NavigationBarTopAdapter ->
                injectedViewAdapter.setNavButtonType(
                    if (GiniCapture.getInstance()
                            .isBottomNavigationBarEnabled
                    ) NavButtonType.NONE else NavButtonType.BACK
                )
                injectedViewAdapter.setTitle(getString(R.string.gc_title_supported_formats))
                injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                })
            }
        }
    }

    private fun setupBottomBarNavigation() {
        val injectedViewContainer = binding.gcInjectedNavigationBarContainerBottom
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            injectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder<HelpNavigationBarBottomAdapter>(
                GiniCapture.getInstance().internal().helpNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter: HelpNavigationBarBottomAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                })
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SupportedFormatsHelpFragment()
    }
}