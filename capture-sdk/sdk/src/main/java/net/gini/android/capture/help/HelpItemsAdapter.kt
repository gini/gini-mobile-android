package net.gini.android.capture.help

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.help.HelpItemsAdapter.HelpItemsViewHolder
import net.gini.android.capture.internal.util.FeatureConfiguration

/**
 * Internal use only.
 *
 * @suppress
 */
internal class HelpItemsAdapter(val onItemSelected: (HelpItem) -> Unit) :
    RecyclerView.Adapter<HelpItemsViewHolder>() {

    val items: List<HelpItem> = mutableListOf<HelpItem>().apply {
        add(HelpItem.PhotoTips)
        if (FeatureConfiguration.isFileImportEnabled()) {
            add(HelpItem.FileImportGuide)
        }
        if (GiniCapture.hasInstance()
            && GiniCapture.getInstance().isSupportedFormatsHelpScreenEnabled
        ) {
            add(HelpItem.SupportedFormats)
        }
        if (GiniCapture.hasInstance()) {
            addAll(GiniCapture.getInstance().customHelpItems)
        }
    }

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
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    internal class HelpItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.gc_help_item_title)
    }
}