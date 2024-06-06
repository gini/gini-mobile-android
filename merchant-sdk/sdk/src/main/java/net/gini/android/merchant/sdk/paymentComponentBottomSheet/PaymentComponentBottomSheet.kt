package net.gini.android.merchant.sdk.paymentComponentBottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetPaymentComponentBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared


class PaymentComponentBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?
): GmsBottomSheetDialogFragment() {
    constructor(): this(null)

    private var binding: GmsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> { PaymentComponentBottomSheetViewModel.Factory(paymentComponent) }
    private var backListener: BackListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GmsBottomSheetPaymentComponentBinding.inflate(inflater, container, false)
        binding.gmsPaymentComponent.paymentComponent = paymentComponent
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backListener?.backCalled()
            }
        })
        return binding.root
    }

    fun setBackListener(backListener: BackListener) {
        this.backListener = backListener
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?) = PaymentComponentBottomSheet(paymentComponent)
    }

}