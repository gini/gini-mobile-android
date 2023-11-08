package net.gini.android.capture.internal.textrecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import net.gini.android.capture.internal.util.Size

/**
 * Created by Alpár Szotyori on 27.04.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

class OCRView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    var text: OCRText? = OCRText(emptyList())
        set(value) {
            field = value
            invalidate()
        }

    var cameraPreviewSize: Size = Size(1, 1)
        set(value) {
            field = value
            scaleX = cameraPreviewSize.width.toFloat() / imageSize.width
            scaleY = cameraPreviewSize.height.toFloat() / imageSize.height
        }
    private var imageSize: Size = Size(1, 1)
        set(value) {
            field = value
            scaleX = cameraPreviewSize.width.toFloat() / imageSize.width
            scaleY = cameraPreviewSize.height.toFloat() / imageSize.height
        }
    private var imageRotation = 0
    @JvmField
    var scaleX = 1f
    @JvmField
    var scaleY = 1f

    var cameraFrame: Rect = Rect()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 4f
        color = Color.parseColor("#88ffffff")
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#00ff00")
    }

    fun setImageSizeAndRotation(size: Size, rotation: Int) {
        imageRotation = rotation % 360
        imageSize = when (imageRotation) {
            90, 270 -> Size(size.height, size.width)
            else -> size
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(cameraFrame, framePaint)

        text?.elements?.forEach { element ->
            val scaledRect = Rect(
                (element.box.left * scaleX).toInt(),
                (element.box.top * scaleY).toInt(),
                (element.box.right * scaleX).toInt(),
                (element.box.bottom * scaleY).toInt()
            )
            canvas.drawRect(scaledRect, paint)
            val textSize = (scaledRect.bottom - scaledRect.top).toFloat() * 0.7f
            canvas.drawText(element.text, scaledRect.left.toFloat(), scaledRect.bottom.toFloat() - ((scaledRect.bottom - scaledRect.top) - textSize), Paint().apply {
                color = Color.BLACK
                this.textSize = textSize
            })
        }
    }
}