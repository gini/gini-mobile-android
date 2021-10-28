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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        getChildAt(0)?.let { child ->
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(child.measuredWidth, child.measuredHeight)
        }
    }
}