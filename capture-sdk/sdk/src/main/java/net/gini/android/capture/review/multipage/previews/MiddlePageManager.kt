package net.gini.android.capture.review.multipage.previews

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.internal.util.AndroidHelper
import net.gini.android.capture.internal.util.ContextHelper

class MiddlePageManager : LinearLayoutManager {

    constructor(context: Context) : super(context)

    private lateinit var recyclerView: RecyclerView


    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )


    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (childCount == 0 && state.itemCount > 0) {
            val firstChild = recycler.getViewForPosition(0)

            measureChildWithMargins(firstChild, 0, 0)
            recycler.recycleView(firstChild)
        }
        super.onLayoutChildren(recycler, state)
    }

    override fun measureChildWithMargins(child: View, widthUsed: Int, heightUsed: Int) {

        val lp = (child.layoutParams as RecyclerView.LayoutParams)

        val lpPos = lp.absoluteAdapterPosition

        super.measureChildWithMargins(child, widthUsed, heightUsed)

        if (lpPos != 0 && lpPos != itemCount - 1) return

        //Post wait to obtain child width
        //Pas the calculation to start and end padding to centre the child
        child.post {
            val hPadding = ((width - child.measuredWidth) / 2).coerceAtLeast(0)
            if (lpPos == 0) recyclerView.updatePaddingRelative(start = hPadding, end = hPadding)
            if (lpPos == itemCount - 1) recyclerView.updatePaddingRelative(end = hPadding)

        }
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        recyclerView = view
        super.onAttachedToWindow(view)
    }
}