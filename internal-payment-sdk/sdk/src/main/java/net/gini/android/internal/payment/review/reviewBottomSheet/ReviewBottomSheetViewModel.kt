package net.gini.android.internal.payment.review.reviewBottomSheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.BackListener


internal class ReviewBottomSheetViewModel private constructor(private val paymentComponent: PaymentComponent, private val reviewConfiguration: ReviewConfiguration, private val giniPaymentModule: GiniInternalPaymentModule, val backListener: BackListener?, val reviewViewListener: ReviewViewListener?): ViewModel() {
    val reviewComponent: ReviewComponent

    init {
        reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig= reviewConfiguration,
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = viewModelScope
        )
    }

    class Factory(private val paymentComponent: PaymentComponent, private val giniPaymentModule: GiniInternalPaymentModule, private val reviewConfiguration: ReviewConfiguration, private val backListener: BackListener?, private val reviewViewListener: ReviewViewListener) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewBottomSheetViewModel(paymentComponent, reviewConfiguration, giniPaymentModule, backListener, reviewViewListener) as T
        }
    }
}