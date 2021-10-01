package net.gini.pay.ginipaybusiness.review.model

import net.gini.pay.ginipaybusiness.review.bank.BankApp

/**
 * A payment request used for starting the bank app. Only the id is sent, but it is associated with a bank.
 */
data class PaymentRequest(
    val id: String,
    val bankApp: BankApp,
)