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
const val PHOTO_TIPS_HEADER = 1
const val PHOTO_TIPS = 2
class PhotoTipsAdapter(private val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //set null to position 0 to serve as placeholder for the header
    private var tipList: MutableList<AnalysisHint?> = mutableListOf(null,
        AnalysisHint.LIGHTING, AnalysisHint.FLAT, AnalysisHint.ALIGN, AnalysisHint.PARALLEL, AnalysisHint.MULTIPAGE)

    init {
        setupItems()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if (viewType == PHOTO_TIPS_HEADER) {
            return PhotoTipsHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.gc_photo_tips_header, parent, false)
            )
        }

        return PhotoTipsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.gc_item_tip, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is PhotoTipsHeaderViewHolder) {
            holder.usefulTips.text = context.getString(R.string.gc_useful_tips)
        }

        if (holder is PhotoTipsViewHolder) {
            val analysisHint = tipList[position]
            analysisHint?.let {
                holder.iconImageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        it.drawableResource
                    )
                )
                holder.tipTextView.text = context.getString(it.textResource)
                holder.tipTitleTextView.text = context.getString(it.titleTextResource)
            }
            if (position == tipList.size - 1) holder.separatorView.visibility =
                View.INVISIBLE else holder.separatorView.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return tipList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0)
            return PHOTO_TIPS_HEADER
        return PHOTO_TIPS
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

    inner class PhotoTipsHeaderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val usefulTips: TextView = itemView.findViewById(R.id.gc_useful_tips);
    }
}

