package net.gini.android.capture.help

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentHelpBinding
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType

/**
 * Internal use only.
 */
class HelpFragment : Fragment() {
    private var binding: GcFragmentHelpBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentHelpBinding.inflate(inflater)
        setUpHelpItems()
        setupTopBarNavigation()
        setupBottomBarNavigation()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (hasOnlyOneHelpItem()) {
            (binding.gcHelpItems.adapter as? HelpItemsAdapter)?.let { adapter ->
                launchHelpScreen(adapter.items[0])
            }
        }
    }

    private fun hasOnlyOneHelpItem(): Boolean {
        return binding.gcHelpItems.adapter?.itemCount == 1
    }

    private fun setUpHelpItems() {
        binding.gcHelpItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HelpItemsAdapter { helpItem -> launchHelpScreen(helpItem) }
        }
    }

    private fun setupTopBarNavigation() {
        val topBarInjectedViewContainer = binding.gcInjectedNavigationBarContainerTop
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(
                    GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
                ) { injectedViewAdapter ->
                    val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                        NavButtonType.NONE else NavButtonType.BACK
                    injectedViewAdapter.setNavButtonType(navType)
                    injectedViewAdapter.setTitle(getString(R.string.gc_title_help))
                    injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                        parentFragmentManager.popBackStack()
                    })
                }
        }
    }

    private fun setupBottomBarNavigation() {
        val injectedViewContainer = binding.gcInjectedNavigationBarContainerBottom
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            injectedViewContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().helpNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    parentFragmentManager.popBackStack()
                })
            }
        }
    }

    private fun launchHelpScreen(helpItem: HelpItem) {
        when (helpItem) {
            HelpItem.PhotoTips -> navigateToFragment(PhotoTipsHelpFragment.newInstance())
            HelpItem.SupportedFormats -> navigateToFragment(SupportedFormatsHelpFragment.newInstance())
            HelpItem.FileImport -> navigateToFragment(FileImportHelpFragment.newInstance())
            is HelpItem.Custom -> requireActivity().startActivity(helpItem.intent)
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .add(R.id.gc_fragment_container, fragment, fragment::class.java.name)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }
}