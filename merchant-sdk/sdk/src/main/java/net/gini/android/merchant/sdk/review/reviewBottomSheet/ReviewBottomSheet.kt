package net.gini.android.merchant.sdk.review.reviewBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetReviewBinding
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.setBackListener

internal class ReviewBottomSheet private constructor(
    internal val paymentButtonListener: ReviewViewListener?,
    private val viewModelFactory: ViewModelProvider.Factory?
) : GmsBottomSheetDialogFragment() {

    constructor(): this(null, null)

    private val viewModel: ReviewBottomSheetViewModel by viewModels { viewModelFactory ?: object : ViewModelProvider.Factory {} }
    private var binding: GmsBottomSheetReviewBinding by autoCleared()

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
    ): View? {
        binding = GmsBottomSheetReviewBinding.inflate(inflater, container, false)
        binding.gmsReviewLayout.reviewComponent = viewModel.reviewComponent
        binding.gmsReviewLayout.listener = paymentButtonListener
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    internal companion object {
        fun newInstance(
            giniMerchant: GiniMerchant,
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewViewListener,
            paymentComponent: PaymentComponent,
            backListener: BackListener,
            viewModelFactory: ViewModelProvider.Factory = ReviewBottomSheetViewModel.Factory(
                paymentComponent = paymentComponent,
                reviewConfiguration = configuration,
                giniMerchant = giniMerchant,
                backListener = backListener
            ),
        ): ReviewBottomSheet = ReviewBottomSheet(listener, viewModelFactory)
    }
}