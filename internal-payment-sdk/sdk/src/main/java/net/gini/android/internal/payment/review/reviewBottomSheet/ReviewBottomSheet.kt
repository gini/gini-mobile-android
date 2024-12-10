package net.gini.android.internal.payment.review.reviewBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.databinding.GpsBottomSheetReviewBinding
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.isLandscapeOrientation
import net.gini.android.internal.payment.utils.extensions.setBackListener


class ReviewBottomSheet private constructor(
    private val viewModelFactory: ViewModelProvider.Factory?
) : GpsBottomSheetDialogFragment() {

    constructor(): this(null)

    private val viewModel: ReviewBottomSheetViewModel by viewModels { viewModelFactory ?: object : ViewModelProvider.Factory {} }
    private var binding: GpsBottomSheetReviewBinding by autoCleared()
    private lateinit var bottomSheet: FrameLayout
    private val listener = object: ReviewViewListener {
        override fun onPaymentButtonTapped(paymentDetails: PaymentDetails) {
            viewModel.reviewViewListener?.onPaymentButtonTapped(paymentDetails)
        }

        override fun onSelectBankButtonTapped() {
            dismiss()
            viewModel.reviewViewListener?.onSelectBankButtonTapped()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            GiniInternalPaymentModule.getSDKLanguage(requireContext())?.languageLocale()
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel.backListener?.let {
            (dialog as BottomSheetDialog).setBackListener(it)
        }

        // For the sake of consistency with the review flow WITH document, disable
        // default draggable behavior on the bottom sheet when in landscape mode
        dialog.setOnShowListener {
            bottomSheet = dialog
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet).apply {
                isDraggable = !resources.isLandscapeOrientation()
                isCancelable = !resources.isLandscapeOrientation()
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GpsBottomSheetReviewBinding.inflate(inflater, container, false)
        binding.gpsReviewLayout.reviewComponent = viewModel.reviewComponent
        binding.gpsReviewLayout.listener = listener
        if (resources.isLandscapeOrientation()) {
            val fieldsLayout = binding.gpsReviewLayout.findViewById<View>(net.gini.android.internal.payment.R.id.gps_fields_layout)
            binding.dragHandle.setOnTouchListener { v, event ->
                // By default, bottom sheet has gravity TOP - layout changes make it jump to the top of the screen
                // set layoutGravity = Gravity.BOTTOM prevents that.
                val layoutParams = bottomSheet.layoutParams as LayoutParams
                layoutParams.gravity = Gravity.BOTTOM
                fieldsLayout.isVisible = !fieldsLayout.isVisible
                false
            }
        }
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    companion object {
        fun newInstance(
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewViewListener,
            giniInternalPaymentModule: GiniInternalPaymentModule,
            backListener: BackListener,
            viewModelFactory: ViewModelProvider.Factory = ReviewBottomSheetViewModel.Factory(
                paymentComponent = giniInternalPaymentModule.paymentComponent,
                reviewConfiguration = configuration,
                giniPaymentModule = giniInternalPaymentModule,
                backListener = backListener,
                reviewViewListener = listener
            ),
        ): ReviewBottomSheet = ReviewBottomSheet(viewModelFactory)
    }
}