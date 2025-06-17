package net.gini.android.internal.payment.paymentComponentBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsBottomSheetPaymentComponentBinding
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentComponentView
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.onKeyboardAction
import net.gini.android.internal.payment.utils.extensions.setBackListener
import org.jetbrains.annotations.VisibleForTesting

class PaymentComponentBottomSheet private constructor(
    paymentComponent: PaymentComponent?,
    reviewFragmentShown: Boolean,
    backListener: BackListener? = null
): GpsBottomSheetDialogFragment() {
    constructor(): this(null, false)

    private var binding: GpsBottomSheetPaymentComponentBinding by autoCleared()
    private val viewModel by viewModels<PaymentComponentBottomSheetViewModel> {
        PaymentComponentBottomSheetViewModel.Factory(
            paymentComponent,
            backListener,
            reviewFragmentShown
        )
    }

    @VisibleForTesting
    internal lateinit var paymentComponentView: PaymentComponentView
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
        binding = GpsBottomSheetPaymentComponentBinding.inflate(inflater, container, false)
        binding.dragHandle.onKeyboardAction {
            dismiss()
        }
        binding.gpsPaymentComponent.paymentComponent = viewModel.paymentComponent
        binding.gpsPaymentComponent.dismissListener = object : PaymentComponentView.ButtonClickListener {
            override fun onButtonClick(button: PaymentComponentView.Buttons) {
                dismiss()
            }

        }
        paymentComponentView = binding.gpsPaymentComponent
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.gps_select_bank_label))
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    companion object {
        fun newInstance(paymentComponent: PaymentComponent?, reviewFragmentShown: Boolean, backListener: BackListener)
            = PaymentComponentBottomSheet(paymentComponent, reviewFragmentShown, backListener)
    }

}
