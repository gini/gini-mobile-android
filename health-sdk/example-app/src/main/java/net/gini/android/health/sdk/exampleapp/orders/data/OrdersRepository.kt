package net.gini.android.health.sdk.exampleapp.orders.data

import kotlinx.coroutines.flow.Flow
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order

class OrdersRepository(
    private val ordersLocalDataSource: OrdersLocalDataSource
) {

    val ordersFlow: Flow<List<Order>> = ordersLocalDataSource.ordersFlow

    suspend fun loadOrders() = ordersLocalDataSource.loadOrders()

    suspend fun updateOrAppendOrder(order: Order) = ordersLocalDataSource.updateOrAppendOrder(order)

    suspend fun deleteRequestIdAndExpiryDate(requestId: String) = ordersLocalDataSource.deleteRequestIdAndExpiryDate(requestId)

    suspend fun convertToPaymentRequest(order: Order, id: String) = ordersLocalDataSource.convertToPaymentRequest(order, id)

    suspend fun deleteRequestIdsAndExpiryDates(orderIds: List<String>) = ordersLocalDataSource.deletePaymentRequestIdsAndExpiryDates(orderIds)

}
