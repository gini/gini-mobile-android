package net.gini.android.merchant.sdk.review.reviewBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.internal.payment.utils.BackListener


internal class ReviewBottomSheetViewModel private constructor(private val paymentComponent: PaymentComponent, private val reviewConfiguration: ReviewConfiguration, private val giniMerchant: GiniMerchant, val backListener: BackListener?): ViewModel() {
    val reviewComponent: ReviewComponent

    init {
        reviewComponent = ReviewComponent(
            paymentComponent = giniMerchant.giniInternalPaymentModule.paymentComponent,
            reviewConfig= reviewConfiguration,
            giniInternalPaymentModule = giniMerchant.giniInternalPaymentModule,
            coroutineScope = viewModelScope
        )
    }

    class Factory(private val paymentComponent: PaymentComponent, private val reviewConfiguration: ReviewConfiguration, private val giniMerchant: GiniMerchant, private val backListener: BackListener?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewBottomSheetViewModel(paymentComponent, reviewConfiguration, giniMerchant, backListener) as T
        }
    }
}