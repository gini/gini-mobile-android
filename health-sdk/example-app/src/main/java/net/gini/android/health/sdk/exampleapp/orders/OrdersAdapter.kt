package net.gini.android.health.sdk.exampleapp.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.orders.data.model.OrderItem


class OrdersAdapter(
    var dataSet: List<OrderItem>,
    private val showOrderDetails: (OrderItem) -> Unit,
) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recipient: TextView = view.findViewById(R.id.recipient)
        val purpose: TextView = view.findViewById(R.id.purpose)
        val amount: TextView = view.findViewById(R.id.amount)
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

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val orderItem = dataSet[position]

        viewHolder.recipient.text = orderItem.recipient
        viewHolder.purpose.text = orderItem.purpose
        viewHolder.amount.text = orderItem.amount

    }

    override fun getItemCount() = dataSet.size
}