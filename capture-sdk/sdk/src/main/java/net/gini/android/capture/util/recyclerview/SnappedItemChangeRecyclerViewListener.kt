package net.gini.android.capture.util.recyclerview

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

internal class SnappedItemChangeRecyclerViewListener(
    private val snapHelper: SnapHelper?,
    private val onItemChanged: (pos: Int) -> Unit,
) : RecyclerView.OnScrollListener() {

    private var lastPosition = POS_UNKNOWN
    private var skipNextEventDetection = false

    fun skipNextEventDetection() {
        skipNextEventDetection = true
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        if (snapHelper == null) return

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {

            if (skipNextEventDetection) {
                // We need to post action because recycler view need a time to finish scrolling
                recyclerView.post {
                    skipNextEventDetection = false
                }
                return
            }

            val layoutManager = recyclerView.layoutManager
                ?: return // skip if no Layout Manager attached

            val snappedView: View = snapHelper.findSnapView(layoutManager)
                ?: return // skip if no view found

            val position: Int = recyclerView.getChildAdapterPosition(snappedView)

            // skip if position wasn't changed
            if (lastPosition == position) return

            onItemChanged(position)

            lastPosition = position
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
    }

    companion object {
        private const val POS_UNKNOWN = -1
    }
}