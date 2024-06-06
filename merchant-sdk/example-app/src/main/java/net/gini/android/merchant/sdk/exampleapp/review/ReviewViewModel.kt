package net.gini.android.merchant.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent

class ReviewViewModel(val giniMerchant: GiniMerchant, val paymentComponent: PaymentComponent) : ViewModel()