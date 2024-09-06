package net.gini.android.internal.payment.paymentComponent

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentComponentConfiguration(
    /**
     * Please contact a Gini representative before changing this configuration option.
     */
    val isPaymentComponentBranded: Boolean = true
) : Parcelable
