package net.gini.android.health.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent

class ReviewViewModel(val giniHealth: GiniHealth, val paymentComponent: PaymentComponent) : ViewModel()