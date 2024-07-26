package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetPaymentComponentBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.setBackListener


class PaymentComponentBottomSheet private constructor(
    paymentComponent: PaymentComponent?,
    reviewFragmentShown: Boolean,
    backListener: BackListener? = null
): GmsBottomSheetDialogFragment() {
    constructor(): this(null, false)

    private var binding: GmsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> { PaymentComponentBottomSheetViewModel.Factory(paymentComponent, backListener, reviewFragmentShown) }

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
        binding.gmsPaymentComponent.getMoreInformationLabel().setOnClickListener {
            viewModel.paymentComponent?.listener?.onMoreInformationClicked()
            dismiss()
        }
        binding.gmsPaymentComponent.getBankPickerButton().setOnClickListener {
            viewModel.paymentComponent?.listener?.onBankPickerClicked()
            dismiss()
        }
        binding.gmsPaymentComponent.getPayInvoiceButton().setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.paymentComponent?.onPayInvoiceClicked()
                // if payment provider does not support GPC and review fragment will not be shown, we're in the case where we show `Open With Bottom Sheet` from the payment component directly
                if (viewModel.paymentProviderApp.value?.paymentProvider?.gpcSupported() == false && !viewModel.reviewFragmentShown) return@launch
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
        internal fun newInstance(paymentComponent: PaymentComponent?, reviewFragmentShown: Boolean, backListener: BackListener) = PaymentComponentBottomSheet(paymentComponent, reviewFragmentShown, backListener)
    }

}