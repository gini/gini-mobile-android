package net.gini.android.health.sdk.review

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.core.api.models.PaymentProvider
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBankSelectionItemBinding
import net.gini.android.health.sdk.databinding.GhsFragmentBankSelectionBinding
import net.gini.android.health.sdk.review.bank.BankApp
import net.gini.android.health.sdk.review.bank.BankAppColors
import net.gini.android.health.sdk.util.autoCleared

/**
 * Created by Alpár Szotyori on 26.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class BankSelectionFragment : BottomSheetDialogFragment() {

    private var binding: GhsFragmentBankSelectionBinding by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GhsFragmentBankSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.banksList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.banksList.adapter = BankAppsAdapter((1..2).map {
            BankApp(
                "Bank Bank Bank $it",
                "$it",
                "",
                ResourcesCompat.getDrawable(Resources.getSystem(), android.R.drawable.ic_dialog_email, null)!!,
                BankAppColors(Color.GREEN, Color.WHITE),
                PaymentProvider("", "", "", ""),
                Intent()
            )
        }
        ).apply {
            selectedBankAppPosition = 0
        }
    }

    override fun getTheme(): Int {
        return R.style.GiniHealth_BankSelection_ThemeOverlay_BottomSheetDialog
    }

    companion object {
        const val TAG = "BankSelectionFragment"
    }
}

class BankAppsAdapter(private val bankApps: List<BankApp>) : RecyclerView.Adapter<BankAppsAdapter.ViewHolder>() {

    var selectedBankAppPosition: Int = 0
        set(value) {
            val prev = selectedBankAppPosition
            field = value
            notifyItemChanged(prev)
            notifyItemChanged(value)
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
            }
        }
    }

    override fun getItemCount(): Int = bankApps.size

    class ViewHolder(val binding: GhsBankSelectionItemBinding) : RecyclerView.ViewHolder(binding.root)
}