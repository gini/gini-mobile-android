package net.gini.android.health.sdk.exampleapp.orders.data.model

import net.gini.android.health.sdk.exampleapp.util.parseAmount

data class OrderItem(
    val order: Order,
    val recipient: String,
    val amount: String,
    val purpose: String
) {

    companion object {
        fun fromOrder(order: Order): OrderItem {
            return OrderItem(
                order = order,
                recipient = order.recipient,
                amount = order.amount.parseAmount(shouldThrowErrorForFormat = false),
                purpose = order.purpose
            )
        }
    }
}