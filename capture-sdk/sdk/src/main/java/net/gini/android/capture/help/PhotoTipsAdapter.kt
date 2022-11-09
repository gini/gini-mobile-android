package net.gini.android.capture.help

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.R
import net.gini.android.capture.analysis.AnalysisHint
import net.gini.android.capture.internal.util.FeatureConfiguration

/**
 * Internal use only.
 *
 * @suppress
 */

class PhotoTipsAdapter(private val context: Context): RecyclerView.Adapter<PhotoTipsAdapter.PhotoTipsViewHolder>() {

    private var tipList: MutableList<AnalysisHint> = mutableListOf(AnalysisHint.LIGHTING, AnalysisHint.FLAT, AnalysisHint.ALIGN, AnalysisHint.PARALLEL, AnalysisHint.MULTIPAGE)

    init {
        setupItems()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoTipsViewHolder =
        PhotoTipsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.gc_item_tip, parent, false))

    override fun onBindViewHolder(holder: PhotoTipsViewHolder, position: Int) {
        holder.iconImageView.setImageDrawable(ContextCompat.getDrawable(context, tipList[position].drawableResource))
        holder.tipTextView.text =  context.getString(tipList[position].textResource)
        holder.tipTitleTextView.text =  context.getString(tipList[position].titleTextResource)
        if (position == tipList.size - 1) holder.separatorView.visibility = View.INVISIBLE else holder.separatorView.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int {
        return tipList.size
    }

    private fun setupItems() {
        if (!FeatureConfiguration.isMultiPageEnabled()) {
            tipList.remove(AnalysisHint.MULTIPAGE)
        }
    }

    inner class PhotoTipsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.gc_tip_photo)
        val tipTitleTextView: TextView = itemView.findViewById(R.id.gc_tip_title)
        val tipTextView: TextView = itemView.findViewById(R.id.gc_tip_text)
        val separatorView: View = itemView.findViewById(R.id.gc_divider)
    }
}

