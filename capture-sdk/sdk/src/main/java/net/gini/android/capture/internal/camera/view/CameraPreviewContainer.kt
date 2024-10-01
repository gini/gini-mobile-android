package net.gini.android.capture.internal.camera.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Internal use only.
 *
 * @suppress
 */
class CameraPreviewContainer : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        getChildAt(0)?.let { child ->
            // Center the child
            val childLeft: Int = ((measuredWidth - child.measuredWidth) / 2f).toInt()
            val childTop: Int = ((measuredHeight - child.measuredHeight) / 2f).toInt()
            child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
        }
    }
}