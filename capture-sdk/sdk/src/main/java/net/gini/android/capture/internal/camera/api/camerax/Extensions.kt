package net.gini.android.capture.internal.camera.api.camerax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.ImageOutputConfig
import net.gini.android.capture.internal.camera.api.CameraException
import java.io.ByteArrayOutputStream
import java.io.IOException

fun ImageProxy.toByteArray(): ByteArray {
    val buffer = planes[0].buffer
    buffer.rewind()
    val byteArray = ByteArray(buffer.remaining())
    buffer.get(byteArray)
    return byteArray
}

fun ImageProxy.shouldCrop(): Boolean {
    val sourceSize = Size(width, height)
    val targetSize = Size(cropRect.width(), cropRect.height())
    return targetSize != sourceSize
}

fun ImageProxy.toCroppedByteArray(): ByteArray {
    val byteArray = toByteArray()
    if (shouldCrop()) {
        return cropByteArray(byteArray, cropRect)
    }
    return byteArray
}

@Throws(CameraException::class)
private fun cropByteArray(data: ByteArray, cropRect: Rect?): ByteArray {
    if (cropRect == null) {
        return data
    }
    try {
        val decoder = BitmapRegionDecoder.newInstance(data, 0, data.size, false)
        val bitmap = decoder.decodeRegion(cropRect, BitmapFactory.Options())
        decoder.recycle()

        val out = ByteArrayOutputStream()
        val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

        if (!success) {
            throw CameraException(
                "Encode bitmap failed.",
                CameraException.Type.SHOT_FAILED
            )
        }

        bitmap.recycle()
        return out.toByteArray()
    } catch (e: IllegalArgumentException) {
        throw CameraException(e, CameraException.Type.SHOT_FAILED)
    } catch (e: IOException) {
        throw CameraException(e, CameraException.Type.SHOT_FAILED)
    }
}

/**
 * Swaps width and height for portrait.
 *
 * @param isPortrait pass in `true` if in portrait orientation
 */
fun Size.forOrientation(isPortrait: Boolean): Size =
    if (isPortrait) {
        Size(height, width)
    } else {
        Size(width, height)
    }
