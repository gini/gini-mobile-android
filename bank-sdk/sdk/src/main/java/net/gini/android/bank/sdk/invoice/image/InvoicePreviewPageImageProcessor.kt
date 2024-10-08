package net.gini.android.bank.sdk.invoice.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import net.gini.android.capture.internal.network.model.DocumentLayout
import net.gini.android.capture.network.model.GiniCaptureBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InvoicePreviewPageImageProcessor {

    companion object {
        var LOG: Logger = LoggerFactory.getLogger(InvoicePreviewPageImageProcessor::class.java)
    }

    suspend fun processImage(
        image: Bitmap,
        skontoPageLayout: DocumentLayout.Page,
        highlightBoxes: List<GiniCaptureBox>,
        color: Int = 0xAAFFFF00.toInt(),
    ): Bitmap = suspendCoroutine { continuation ->

        val finalBitmap = image.copy(Bitmap.Config.ARGB_8888, true)

        val scaleY = image.height.toFloat() / skontoPageLayout.sizeY
        val scaleX = image.width.toFloat() / skontoPageLayout.sizeX

        val canvas = Canvas(finalBitmap)

        val scaledBoxes = highlightBoxes.map { it.scale(scaleX, scaleY) }

        val scaledRectList = scaledBoxes.map { it.toRect() }

        val paint = Paint().apply {
            this.color = color
        }

        if (scaledRectList.isNotEmpty()) {
            canvas.drawHighlightRect(scaledRectList.unionAll(), paint)
        } else {
            LOG.debug("No boxes to highlight detected")
        }

        continuation.resume(finalBitmap)
    }
}

private fun Canvas.drawHighlightRect(rect: RectF, paint: Paint) {
    drawRect(rect, paint)
}

private fun List<RectF>.unionAll() = RectF(
    minOf { it.left },
    minOf { it.top },
    maxOf { it.right },
    maxOf { it.bottom },
)

private fun GiniCaptureBox.toRect() = RectF(
    left.toFloat(),
    top.toFloat(),
    left.toFloat() + width.toFloat(),
    top.toFloat() + height.toFloat(),
)

private fun GiniCaptureBox.scale(scaleX: Float, scaleY: Float) = GiniCaptureBox(
    pageNumber,
    left * scaleX,
    top * scaleY,
    width * scaleX,
    height * scaleY,
)
