package net.gini.android.capture.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcFragmentHelpBinding
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType

/**
 * Internal use only.
 */
class HelpFragment : Fragment() {
    private var binding: GcFragmentHelpBinding by autoCleared()

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentHelpBinding.inflate(inflater)
        setUpHelpItems()
        setupTopBarNavigation()
        setupBottomBarNavigation()
        handleOnBackPressed()
        return binding.root
    }

    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(HelpFragmentDirections.toCameraFragment())
                isEnabled = false
                remove()
            }
        })
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
                        findNavController().navigate(HelpFragmentDirections.toCameraFragment())
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
                    findNavController().navigate(HelpFragmentDirections.toCameraFragment())
                })
            }
        }
    }

    private fun launchHelpScreen(helpItem: HelpItem) {
        when (helpItem) {
            HelpItem.PhotoTips -> findNavController().navigate(HelpFragmentDirections.toPhotoTipsHelpFragment())
            HelpItem.SupportedFormats -> findNavController().navigate(HelpFragmentDirections.toSupportedFormatsHelpFragment())
            HelpItem.FileImport -> findNavController().navigate(HelpFragmentDirections.toFileImportHelpFragment())
            is HelpItem.Custom -> requireActivity().startActivity(helpItem.intent)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }
}