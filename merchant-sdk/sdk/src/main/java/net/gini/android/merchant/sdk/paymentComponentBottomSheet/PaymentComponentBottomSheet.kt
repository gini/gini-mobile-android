package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetPaymentComponentBinding
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared


class PaymentComponentBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?,
    private val documentId: String,
    backListener: BackListener? = null
): GmsBottomSheetDialogFragment(backListener) {
    constructor(): this(null, "")

    private var binding: GmsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> { PaymentComponentBottomSheetViewModel.Factory(paymentComponent) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GmsBottomSheetPaymentComponentBinding.inflate(inflater, container, false)
        binding.gmsPaymentComponent.paymentComponent = paymentComponent
        binding.gmsPaymentComponent.findViewById<TextView>(R.id.gms_more_information).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onMoreInformationClicked()
        }
        binding.gmsPaymentComponent.findViewById<TextView>(R.id.gms_select_bank_button).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onBankPickerClicked()
        }
        binding.gmsPaymentComponent.findViewById<Button>(R.id.gms_pay_invoice_button).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onPayInvoiceClicked(documentId)
        }
        return binding.root
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?, documentId: String, backListener: BackListener) = PaymentComponentBottomSheet(paymentComponent, documentId,backListener)
    }

}