package net.gini.android.merchant.sdk.exampleapp.orders.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.gini.android.merchant.sdk.exampleapp.orders.data.model.Order

class OrdersRepository(
    hardcodedOrdersLocalDataSource: HardcodedOrdersLocalDataSource
) {

    val ordersFlow: Flow<List<Order>> = flow {
        emit(hardcodedOrdersLocalDataSource.getOrders())
    }

}
