package net.gini.android.health.sdk.review

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBankSelectionItemBinding
import net.gini.android.health.sdk.databinding.GhsFragmentBankSelectionBinding
import net.gini.android.health.sdk.review.bank.BankApp
import net.gini.android.health.sdk.util.autoCleared
import kotlin.math.max

/**
 * Created by Alpár Szotyori on 26.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class BankSelectionFragment : BottomSheetDialogFragment() {

    private val viewModel: ReviewViewModel by activityViewModels()
    private var binding: GhsFragmentBankSelectionBinding by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GhsFragmentBankSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.banksList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = BankAppsAdapter(emptyList())
            itemAnimator?.changeDuration = 0
        }

        // React to changes from the view model
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.bankApps.combine(viewModel.selectedBank) { bankAppsState, selectedBank -> bankAppsState to selectedBank }
                .collect { (bankAppsState, selectedBank) ->
                    when (bankAppsState) {
                        is ReviewViewModel.BankAppsState.Success -> {
                            (binding.banksList.adapter as BankAppsAdapter).apply {
                                bankApps = bankAppsState.bankApps
                                selectedBank?.let { setSelectedBank(it) }
                            }
                        }
                        else -> {} // Ignored
                    }
                }
        }

        // React to changes from the adapter
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            (binding.banksList.adapter as BankAppsAdapter).userSelectedBankApp.collect {
                viewModel.setSelectedBank(it)
                requireParentFragment().childFragmentManager.commit {
                    remove(this@BankSelectionFragment)
                }
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.GiniHealth_BankSelection_ThemeOverlay_BottomSheetDialog
    }

    companion object {
        const val TAG = "BankSelectionFragment"
    }
}

class BankAppsAdapter(bankApps: List<BankApp>) : RecyclerView.Adapter<BankAppsAdapter.ViewHolder>() {

    var bankApps: List<BankApp> = bankApps
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedBankAppPosition: Int = 0
        set(value) {
            val prev = selectedBankAppPosition
            field = value
            notifyItemChanged(prev)
            notifyItemChanged(value)
        }

    private val _userSelectedBankApp: MutableSharedFlow<BankApp> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val userSelectedBankApp: SharedFlow<BankApp> = _userSelectedBankApp

    fun setSelectedBank(selectedBank: BankApp) {
        selectedBankAppPosition = max(0, bankApps.indexOf(selectedBank))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(GhsBankSelectionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bankApp = bankApps[position]
        with(holder.binding) {
            icon.setImageDrawable(bankApp.icon)
            text.text = bankApp.name
            checkmark.setImageDrawable(
                if (bankApp.packageName == bankApps[selectedBankAppPosition].packageName) ResourcesCompat.getDrawable(
                    Resources.getSystem(),
                    android.R.drawable.ic_input_add,
                    null
                )!! else null
            )
            separator.isVisible = position != bankApps.lastIndex
            root.setOnClickListener {
                selectedBankAppPosition = holder.adapterPosition
                _userSelectedBankApp.tryEmit(bankApps[holder.adapterPosition])
            }
        }
    }

    override fun getItemCount(): Int = bankApps.size

    class ViewHolder(val binding: GhsBankSelectionItemBinding) : RecyclerView.ViewHolder(binding.root)
}