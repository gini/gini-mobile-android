package net.gini.android.capture.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
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

    private val viewModel: HelpViewModel by viewModels()

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
        observeSideEffects()
        viewModel.onScreenShown(resolvedHelpItemTitles())
        return binding.root
    }

    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.onBackClicked()
                    isEnabled = false
                    remove()
                }
            })
    }


    override fun onStart() {
        super.onStart()
        viewModel.onStarted()
    }

    private fun observeSideEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sideEffects.collect { sideEffect ->
                    when (sideEffect) {
                        is HelpSideEffect.NavigateToHelpItem -> launchHelpScreen(sideEffect.helpItem)
                        HelpSideEffect.NavigateBackToCamera ->
                            findNavController().navigate(HelpFragmentDirections.toCameraFragment())
                    }
                }
            }
        }
    }

    private fun setUpHelpItems() {
        binding.gcHelpItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HelpItemsAdapter(viewModel.uiState.value.helpItems) { helpItem ->
                viewModel.onHelpItemClicked(helpItem, getString(helpItem.title))
            }
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
                        viewModel.onBackClicked()
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
                    viewModel.onBackClicked()
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

    private fun resolvedHelpItemTitles(): List<String> =
        viewModel.uiState.value.helpItems.map { getString(it.title) }

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }
}
