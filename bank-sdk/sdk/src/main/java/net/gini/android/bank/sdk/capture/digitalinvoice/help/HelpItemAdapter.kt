package net.gini.android.bank.sdk.capture.digitalinvoice.help

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.bank.sdk.R

/**
 * Internal use only.
 *
 */
class HelpItemAdapter(private val context: Context): RecyclerView.Adapter<HelpItemAdapter.HelpViewHolder>() {

    private var tipList: MutableList<HelpItem> = mutableListOf(HelpItem.DIGITAL_INVOICE, HelpItem.EDIT, HelpItem.SHOP)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder =
        HelpViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.gbs_item_help, parent, false))

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        holder.iconImageView.setImageDrawable(ContextCompat.getDrawable(context, tipList[position].drawableResource))
        holder.iconImageView.contentDescription = context.getString(tipList[position].iconContentTextResource)
        holder.helpTextView.text =  context.getString(tipList[position].textResource)
        holder.helpTitleTextView.text =  "${position + 1}. " + context.getString(tipList[position].titleTextResource)
    }

    override fun getItemCount(): Int {
        return tipList.size
    }

    inner class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.gbs_help_icon)
        val helpTitleTextView: TextView = itemView.findViewById(R.id.gbs_help_title)
        val helpTextView: TextView = itemView.findViewById(R.id.gbs_help_text)
    }
}
