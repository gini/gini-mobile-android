package net.gini.android.capture.help

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.R
import net.gini.android.capture.analysis.AnalysisHint
import net.gini.android.capture.internal.util.FeatureConfiguration
import java.util.*

/**
 * Internal use only.
 *
 * @suppress
 */

class PhotoTipsAdapter: RecyclerView.Adapter<PhotoTipsAdapter.PhotoTipsViewHolder>() {

    private var tipList: MutableList<AnalysisHint> = mutableListOf(AnalysisHint.LIGHTING, AnalysisHint.FLAT, AnalysisHint.ALIGN, AnalysisHint.PARALLEL, AnalysisHint.MULTIPAGE)

    init {
        setupItems()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoTipsViewHolder =
        PhotoTipsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.gc_item_tip, parent, false))

    override fun onBindViewHolder(holder: PhotoTipsViewHolder, position: Int) {
        
    }

    override fun getItemCount(): Int {
        return tipList.size
    }

    private fun setupItems() {
        if (!FeatureConfiguration.isMultiPageEnabled()) {
            tipList.remove(AnalysisHint.MULTIPAGE)
        }
    }

    class PhotoTipsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconImageView: ImageView = itemView.findViewById(R.id.gc_tip_photo) as ImageView
        var tipTitleTextView: TextView = itemView.findViewById<View>(R.id.gc_tip_title) as TextView
        var tipTextView: TextView = itemView.findViewById<View>(R.id.gc_tip_title) as TextView
    }
}

