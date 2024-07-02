package net.gini.android.health.sdk.bankselection

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetBankSelectionBinding
import net.gini.android.health.sdk.databinding.GhsItemPaymentProviderAppBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.GhsBottomSheetDialogFragment
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme
import net.gini.android.health.sdk.util.setIntervalClickListener
import net.gini.android.health.sdk.util.wrappedWithGiniHealthTheme
import org.slf4j.LoggerFactory

/**
 * The [BankSelectionBottomSheet] displays a list of available banks for the user to choose from. If a banking app is not
 * installed it will also display its Play Store link.
 */
class BankSelectionBottomSheet private constructor(private val paymentComponent: PaymentComponent?) :
    GhsBottomSheetDialogFragment() {

    constructor() : this(null)

    private var binding: GhsBottomSheetBankSelectionBinding by autoCleared()
    private val viewModel: BankSelectionViewModel by viewModels { BankSelectionViewModel.Factory(paymentComponent) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = GhsBottomSheetBankSelectionBinding.inflate(inflater, container, false)

        binding.ghsPaymentProviderAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.ghsPaymentProviderAppsList.adapter =
            PaymentProviderAppsAdapter(emptyList(), object : PaymentProviderAppsAdapter.OnItemClickListener {
                override fun onItemClick(paymentProviderApp: PaymentProviderApp) {
                    LOG.debug("Selected payment provider app: {}", paymentProviderApp.name)

                    viewModel.setSelectedPaymentProviderApp(paymentProviderApp)
                    this@BankSelectionBottomSheet.dismiss()
                }
            })

        binding.ghsCloseButton.setOnClickListener {
            dismiss()
        }

        binding.ghsMoreInformationLabel.apply {
            paintFlags = binding.ghsMoreInformationLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                viewModel.paymentComponent?.listener?.onMoreInformationClicked()
                dismiss()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentProviderAppsListFlow.collect { paymentProviderAppsListState ->
                    when (paymentProviderAppsListState) {
                        is PaymentProviderAppsListState.Error -> {
                            dismiss()
                        }

                        PaymentProviderAppsListState.Loading -> {}

                        is PaymentProviderAppsListState.Success -> {
                            (binding.ghsPaymentProviderAppsList.adapter as PaymentProviderAppsAdapter).apply {
                                dataSet = paymentProviderAppsListState.paymentProviderAppsList
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.recheckWhichPaymentProviderAppsAreInstalled()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BankSelectionBottomSheet::class.java)

        /**
         * Create a new instance of the [BankSelectionBottomSheet].
         *
         * @param paymentComponent the [PaymentComponent] which contains the list of payment provider apps and handles the
         * payment provider app selection
         */
        fun newInstance(paymentComponent: PaymentComponent): BankSelectionBottomSheet {
            return BankSelectionBottomSheet(paymentComponent)
        }
    }
}

internal class PaymentProviderAppsAdapter(
    var dataSet: List<PaymentProviderAppListItem>,
    val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<PaymentProviderAppsAdapter.ViewHolder>() {

    class ViewHolder(binding: GhsItemPaymentProviderAppBinding, onClickListener: OnClickListener) : RecyclerView.ViewHolder(binding.root) {
        val button: Button
        val iconView: ShapeableImageView

        init {
            iconView = binding.ghsSelectorLayout.ghsPaymentProviderAppIconHolder.ghsPaymentProviderIcon
            button = binding.ghsSelectorLayout.ghsSelectBankButton
            button.setIntervalClickListener { onClickListener.onClick(adapterPosition) }
        }

        interface OnClickListener {
            fun onClick(adapterPosition: Int)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = GhsItemPaymentProviderAppBinding.inflate(parent.getLayoutInflaterWithGiniHealthTheme(), parent, false)
        val viewHolder = ViewHolder(view, object : ViewHolder.OnClickListener {
            override fun onClick(adapterPosition: Int) {
                onItemClickListener.onItemClick(dataSet[adapterPosition].paymentProviderApp)
            }
        })
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paymentProviderAppListItem = dataSet[position]
        holder.itemView.context.wrappedWithGiniHealthTheme().let { context ->
            holder.button.text = paymentProviderAppListItem.paymentProviderApp.name
            holder.iconView.setImageDrawable(paymentProviderAppListItem.paymentProviderApp.icon)
            holder.itemView.isSelected = paymentProviderAppListItem.isSelected
            holder.button.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                if (paymentProviderAppListItem.isSelected) ContextCompat.getDrawable(context, R.drawable.ghs_checkmark) else null,
                null
            )
        }
    }

    override fun getItemCount() = dataSet.size

    interface OnItemClickListener {
        fun onItemClick(paymentProviderApp: PaymentProviderApp)
    }
}