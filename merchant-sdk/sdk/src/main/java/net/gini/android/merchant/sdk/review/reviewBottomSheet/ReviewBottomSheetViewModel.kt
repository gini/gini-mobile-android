package net.gini.android.merchant.sdk.review.reviewBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.review.ReviewConfiguration
import net.gini.android.merchant.sdk.review.reviewComponent.ReviewComponent
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GiniPaymentManager


internal class ReviewBottomSheetViewModel private constructor(private val paymentComponent: PaymentComponent, private val reviewConfiguration: ReviewConfiguration, private val giniMerchant: GiniMerchant, private val giniPaymentManager: GiniPaymentManager, val backListener: BackListener?): ViewModel() {
    val reviewComponent: ReviewComponent

    init {
        reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig= reviewConfiguration,
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPaymentManager,
            coroutineScope = viewModelScope
        )
    }

    class Factory(private val paymentComponent: PaymentComponent, private val reviewConfiguration: ReviewConfiguration, private val giniMerchant: GiniMerchant, private val giniPaymentManager: GiniPaymentManager, private val backListener: BackListener?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewBottomSheetViewModel(paymentComponent, reviewConfiguration, giniMerchant, giniPaymentManager, backListener) as T
        }
    }
}