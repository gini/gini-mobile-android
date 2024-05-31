package net.gini.android.merchant.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import net.gini.android.merchant.sdk.GiniHealth
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent

class ReviewViewModel(val giniHealth: GiniHealth, val paymentComponent: PaymentComponent) : ViewModel()