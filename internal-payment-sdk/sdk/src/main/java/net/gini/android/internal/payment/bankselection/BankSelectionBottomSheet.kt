package net.gini.android.internal.payment.bankselection

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
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsBottomSheetBankSelectionBinding
import net.gini.android.internal.payment.databinding.GpsItemPaymentProviderAppBinding
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentTheme
import net.gini.android.internal.payment.utils.extensions.setBackListener
import net.gini.android.internal.payment.utils.extensions.setIntervalClickListener
import net.gini.android.internal.payment.utils.extensions.wrappedWithGiniPaymentTheme
import org.slf4j.LoggerFactory

/**
 * The [BankSelectionBottomSheet] displays a list of available banks for the user to choose from. If a banking app is not
 * installed it will also display its Play Store link.
 */
class BankSelectionBottomSheet private constructor(private val paymentComponent: PaymentComponent?, private val backListener: BackListener? = null) :
    GpsBottomSheetDialogFragment() {

    constructor() : this(null)

    private var binding: GpsBottomSheetBankSelectionBinding by autoCleared()
    private val viewModel: BankSelectionViewModel by viewModels {
        BankSelectionViewModel.Factory(
            paymentComponent,
            backListener
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = GpsBottomSheetBankSelectionBinding.inflate(inflater, container, false)

        binding.gpsPaymentProviderAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.gpsPaymentProviderAppsList.adapter =
            PaymentProviderAppsAdapter(emptyList(), object : PaymentProviderAppsAdapter.OnItemClickListener {
                override fun onItemClick(paymentProviderApp: PaymentProviderApp) {
                    LOG.debug("Selected payment provider app: {}", paymentProviderApp.name)

                    viewModel.setSelectedPaymentProviderApp(paymentProviderApp)
                    this@BankSelectionBottomSheet.dismiss()
                    viewModel.backListener?.backCalled()
                }
            })

        binding.gpsCloseButton.setOnClickListener {
            viewModel.backListener?.backCalled()
            dismiss()
        }

        binding.gpsMoreInformationLabel.apply {
            paintFlags = binding.gpsMoreInformationLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
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
                            (binding.gpsPaymentProviderAppsList.adapter as PaymentProviderAppsAdapter).apply {
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

    class ViewHolder(binding: GpsItemPaymentProviderAppBinding, onClickListener: OnClickListener) : RecyclerView.ViewHolder(binding.root) {
        val button: Button
        val iconView: ShapeableImageView

        init {
            iconView = binding.gpsSelectorLayout.gpsPaymentProviderAppIconHolder.gpsPaymentProviderIcon
            button = binding.gpsSelectorLayout.gpsSelectBankButton
            button.setIntervalClickListener { onClickListener.onClick(adapterPosition) }
        }

        interface OnClickListener {
            fun onClick(adapterPosition: Int)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = GpsItemPaymentProviderAppBinding.inflate(parent.getLayoutInflaterWithGiniPaymentTheme(), parent, false)
        val viewHolder = ViewHolder(view, object : ViewHolder.OnClickListener {
            override fun onClick(adapterPosition: Int) {
                onItemClickListener.onItemClick(dataSet[adapterPosition].paymentProviderApp)
            }
        })
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paymentProviderAppListItem = dataSet[position]
        holder.itemView.context.wrappedWithGiniPaymentTheme().let { context ->
            holder.button.text = paymentProviderAppListItem.paymentProviderApp.name
            holder.iconView.setImageDrawable(paymentProviderAppListItem.paymentProviderApp.icon)
            holder.itemView.isSelected = paymentProviderAppListItem.isSelected
            holder.button.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                if (paymentProviderAppListItem.isSelected) ContextCompat.getDrawable(context, R.drawable.gps_checkmark) else null,
                null
            )
        }
    }

    override fun getItemCount() = dataSet.size

    interface OnItemClickListener {
        fun onItemClick(paymentProviderApp: PaymentProviderApp)
    }
}