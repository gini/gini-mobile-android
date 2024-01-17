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
import net.gini.android.capture.databinding.GcFragmentPhotoTipsHelpBinding
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.util.autoCleared
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Internal use only.
 */
class PhotoTipsHelpFragment : Fragment() {
    private var binding: GcFragmentPhotoTipsHelpBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GcFragmentPhotoTipsHelpBinding.inflate(inflater)
        setupTipList()
        setupTopBarNavigation()
        setupBottomBarNavigation()
        return binding.root
    }

    private fun setupTipList() {
        val recyclerView: RecyclerView = binding.gcTipsRecycleview
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = PhotoTipsAdapter(requireContext(), false)
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
                injectedViewAdapter.setTitle(getString(R.string.gc_title_photo_tips))
                injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                    findNavController().popBackStack()
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
                    findNavController().popBackStack()
                })
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PhotoTipsHelpFragment()
    }
}