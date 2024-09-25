package net.gini.android.merchant.sdk.bankselection

import android.app.Dialog
import android.content.DialogInterface
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetBankSelectionBinding
import net.gini.android.merchant.sdk.databinding.GmsItemPaymentProviderAppBinding
import net.gini.android.merchant.sdk.integratedFlow.PaymentFragment
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.setBackListener
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.setIntervalClickListener
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme
import org.slf4j.LoggerFactory

/**
 * The [BankSelectionBottomSheet] displays a list of available banks for the user to choose from. If a banking app is not
 * installed it will also display its Play Store link.
 */
internal class BankSelectionBottomSheet private constructor(private val paymentComponent: PaymentComponent?, private val backListener: BackListener? = null) :
    GmsBottomSheetDialogFragment() {

    constructor() : this(null)

    private var binding: GmsBottomSheetBankSelectionBinding by autoCleared()
    private val viewModel: BankSelectionViewModel by viewModels { BankSelectionViewModel.Factory(paymentComponent, backListener) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = GmsBottomSheetBankSelectionBinding.inflate(inflater, container, false)

        binding.gmsPaymentProviderAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.gmsPaymentProviderAppsList.adapter =
            PaymentProviderAppsAdapter(emptyList(), object : PaymentProviderAppsAdapter.OnItemClickListener {
                override fun onItemClick(paymentProviderApp: PaymentProviderApp) {
                    LOG.debug("Selected payment provider app: {}", paymentProviderApp.name)

                    viewModel.setSelectedPaymentProviderApp(paymentProviderApp)
                    this@BankSelectionBottomSheet.dismiss()
                    (this@BankSelectionBottomSheet.parentFragment as PaymentFragment).handleBackFlow()
                }
            })

        binding.gmsCloseButton.setOnClickListener {
            viewModel.backListener?.backCalled()
            dismiss()
        }

        binding.gmsMoreInformationLabel.apply {
            paintFlags = binding.gmsMoreInformationLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
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
                            (binding.gmsPaymentProviderAppsList.adapter as PaymentProviderAppsAdapter).apply {
                                dataSet = paymentProviderAppsListState.paymentProviderAppsList
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    override fun onStart() {
        super.onStart()
        viewModel.recheckWhichPaymentProviderAppsAreInstalled()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel.backListener?.let {
            (dialog as BottomSheetDialog).setBackListener(it)
        }
        return dialog
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BankSelectionBottomSheet::class.java)

        /**
         * Create a new instance of the [BankSelectionBottomSheet].
         *
         * @param paymentComponent the [PaymentComponent] which contains the list of payment provider apps and handles the
         * payment provider app selection
         */
        fun newInstance(paymentComponent: PaymentComponent, backListener: BackListener? = null): BankSelectionBottomSheet {
            return BankSelectionBottomSheet(paymentComponent, backListener)
        }
    }
}

internal class PaymentProviderAppsAdapter(
    var dataSet: List<PaymentProviderAppListItem>,
    val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<PaymentProviderAppsAdapter.ViewHolder>() {

    class ViewHolder(binding: GmsItemPaymentProviderAppBinding, onClickListener: OnClickListener) : RecyclerView.ViewHolder(binding.root) {
        val button: Button
        val iconView: ShapeableImageView

        init {
            iconView = binding.gmsSelectorLayout.gmsPaymentProviderAppIconHolder.gmsPaymentProviderIcon
            button = binding.gmsSelectorLayout.gmsSelectBankButton
            button.setIntervalClickListener { onClickListener.onClick(adapterPosition) }
        }

        interface OnClickListener {
            fun onClick(adapterPosition: Int)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = GmsItemPaymentProviderAppBinding.inflate(parent.getLayoutInflaterWithGiniMerchantTheme(), parent, false)
        val viewHolder = ViewHolder(view, object : ViewHolder.OnClickListener {
            override fun onClick(adapterPosition: Int) {
                onItemClickListener.onItemClick(dataSet[adapterPosition].paymentProviderApp)
            }
        })
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paymentProviderAppListItem = dataSet[position]
        holder.itemView.context.wrappedWithGiniMerchantTheme().let { context ->
            holder.button.text = paymentProviderAppListItem.paymentProviderApp.name
            holder.iconView.setImageDrawable(paymentProviderAppListItem.paymentProviderApp.icon)
            holder.itemView.isSelected = paymentProviderAppListItem.isSelected
            holder.button.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                if (paymentProviderAppListItem.isSelected) ContextCompat.getDrawable(context, R.drawable.gms_checkmark) else null,
                null
            )
        }
    }

    override fun getItemCount() = dataSet.size

    interface OnItemClickListener {
        fun onItemClick(paymentProviderApp: PaymentProviderApp)
    }
}