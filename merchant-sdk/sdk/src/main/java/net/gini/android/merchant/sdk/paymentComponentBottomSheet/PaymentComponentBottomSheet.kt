package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GhsBottomSheetPaymentComponentBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GhsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared


class PaymentComponentBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?,
    private val documentId: String,
    backListener: BackListener? = null
): GhsBottomSheetDialogFragment(backListener) {
    constructor(): this(null, "")

    private var binding: GhsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> { PaymentComponentBottomSheetViewModel.Factory(paymentComponent) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GhsBottomSheetPaymentComponentBinding.inflate(inflater, container, false)
        binding.ghsPaymentComponent.paymentComponent = paymentComponent
        binding.ghsPaymentComponent.findViewById<TextView>(R.id.ghs_more_information).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onMoreInformationClicked()
        }
        binding.ghsPaymentComponent.findViewById<TextView>(R.id.ghs_select_bank_button).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onBankPickerClicked()
        }
        binding.ghsPaymentComponent.findViewById<Button>(R.id.ghs_pay_invoice_button).setOnClickListener {
            dismiss()
            paymentComponent?.listener?.onPayInvoiceClicked(documentId)
        }
        return binding.root
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?, documentId: String, backListener: BackListener) = PaymentComponentBottomSheet(paymentComponent, documentId,backListener)
    }

}