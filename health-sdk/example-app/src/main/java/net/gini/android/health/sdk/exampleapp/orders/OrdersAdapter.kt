package net.gini.android.health.sdk.exampleapp.orders

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.orders.data.model.OrderItem
import net.gini.android.health.sdk.exampleapp.util.isInTheFuture

class OrdersAdapter(
    var dataSet: List<OrderItem>,
    private val showOrderDetails: (OrderItem) -> Unit,
    private val deletePaymentRequest: (String) -> Unit
) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipient: TextView = view.findViewById(R.id.recipient)
        val purpose: TextView = view.findViewById(R.id.purpose)
        val amount: TextView = view.findViewById(R.id.amount)
        val deleteBtn: Button = view.findViewById(R.id.delete_button)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_order, viewGroup, false)
        return ViewHolder(view).also { vh ->
            vh.itemView.setOnClickListener {
                showOrderDetails(dataSet[vh.adapterPosition])
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val orderItem = dataSet[position]

        viewHolder.recipient.text = orderItem.recipient
        viewHolder.purpose.text = orderItem.purpose
        viewHolder.amount.text = orderItem.amount

        val requestId = orderItem.order.requestId
        if (!requestId.isNullOrEmpty() && orderItem.order.expiryDate.isInTheFuture()) {
            viewHolder.deleteBtn.visibility = View.VISIBLE
            viewHolder.deleteBtn.setOnClickListener {
                deletePaymentRequest(requestId)
            }
        } else {
            viewHolder.deleteBtn.visibility = View.GONE
        }
    }

    override fun getItemCount() = dataSet.size
}