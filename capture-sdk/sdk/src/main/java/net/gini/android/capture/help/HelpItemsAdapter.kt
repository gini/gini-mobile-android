package net.gini.android.capture.help

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.R
import net.gini.android.capture.help.HelpItemsAdapter.HelpItemsViewHolder

/**
 * Internal use only.
 *
 * @suppress
 */
internal class HelpItemsAdapter(
    val items: List<HelpItem>,
    val onItemSelected: (HelpItem) -> Unit
) : RecyclerView.Adapter<HelpItemsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.gc_item_help,
            parent, false
        )
        return HelpItemsViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpItemsViewHolder, position: Int) {
        with(holder) {
            title.setText(items[position].title)
            itemView.setOnClickListener {
                onItemSelected(items[bindingAdapterPosition])
            }

            //Remove last divider from the screen
            if (position == items.size - 1) {
                divider.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    internal class HelpItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.gc_help_item_title)
        val divider: View = itemView.findViewById(R.id.gc_help_item_divider)
    }
}