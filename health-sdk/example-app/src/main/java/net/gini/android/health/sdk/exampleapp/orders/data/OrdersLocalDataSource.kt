package net.gini.android.health.sdk.exampleapp.orders.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order
import net.gini.android.internal.payment.utils.extensions.toBackendFormat

private val Context.dataStore by preferencesDataStore(name = "orders")
private val KEY_ORDERS = stringPreferencesKey("orders")

class OrdersLocalDataSource(private val context: Context, val hardcodedOrdersLocalDataSource: HardcodedOrdersLocalDataSource) {

    private val _ordersFlow: MutableStateFlow<List<Order>> =
        MutableStateFlow(listOf())
    val ordersFlow = _ordersFlow.asStateFlow()

    private val moshi: Moshi = Moshi.Builder().build()

    @OptIn(ExperimentalStdlibApi::class)
    private val jsonAdapter: JsonAdapter<List<Order>> =
        moshi.adapter<List<Order>>()

    suspend fun loadOrders() {
        _ordersFlow.value = readOrdersFromPreferences()
    }

    suspend fun updateOrAppendOrder(order: Order) {
        var documentsList = readOrdersFromPreferences()
        val document =
            documentsList.firstOrNull { it.id == order.id }
        document?.let {
            it.amount = order.amount.toBackendFormat()
            it.recipient = order.recipient
            it.iban = order.iban
            it.purpose = order.purpose

        } ?: run {
            documentsList = documentsList.toMutableList().apply {
                add(order.copy(amount = order.amount.toBackendFormat()))
            }
        }
        writeOrdersToPreferences(documentsList)
        _ordersFlow.value = documentsList
    }

    private suspend fun readOrdersFromPreferences(): List<Order> {
        return context.dataStore.data.map { preferences ->
            val invoicesJson = preferences[KEY_ORDERS] ?: ""
            if (invoicesJson.isNotEmpty()) {
                jsonAdapter.fromJson(invoicesJson) ?: emptyList()
            } else {
                val ordersList = hardcodedOrdersLocalDataSource.getOrders()
                writeOrdersToPreferences(ordersList)
                ordersList
            }
        }.first()
    }

    private suspend fun writeOrdersToPreferences(invoices: List<Order>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ORDERS] = jsonAdapter.toJson(invoices)
        }
    }
}