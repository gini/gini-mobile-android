package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetPaymentComponentBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.setBackListener


class PaymentComponentBottomSheet private constructor(
    paymentComponent: PaymentComponent?,
    documentId: String,
    backListener: BackListener? = null
): GmsBottomSheetDialogFragment() {
    constructor(): this(null, "")

    private var binding: GmsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> { PaymentComponentBottomSheetViewModel.Factory(paymentComponent, backListener, documentId) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel.backListener?.let {
            (dialog as BottomSheetDialog).setBackListener(it)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GmsBottomSheetPaymentComponentBinding.inflate(inflater, container, false)
        binding.gmsPaymentComponent.paymentComponent = viewModel.paymentComponent
        binding.gmsPaymentComponent.findViewById<TextView>(R.id.gms_more_information).setOnClickListener {
            viewModel.paymentComponent?.listener?.onMoreInformationClicked()
            dismiss()
        }
        binding.gmsPaymentComponent.findViewById<TextView>(R.id.gms_select_bank_button).setOnClickListener {
            viewModel.paymentComponent?.listener?.onBankPickerClicked()
            dismiss()
        }
        binding.gmsPaymentComponent.findViewById<Button>(R.id.gms_pay_invoice_button).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.paymentComponent?.onPayInvoiceClicked(viewModel.documentId)
                dismiss()
            }
        }
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    companion object {
        internal fun newInstance(paymentComponent: PaymentComponent?, documentId: String, backListener: BackListener) = PaymentComponentBottomSheet(paymentComponent, documentId,backListener)
    }

}