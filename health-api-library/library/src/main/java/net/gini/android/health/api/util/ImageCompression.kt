package net.gini.android.health.api.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

private const val FILE_SIZE_LIMIT: Int = 10_485_760 // 10MB
private const val DEFAULT_JPEG_COMPRESSION_QUALITY = 50

internal class ImageCompression {

    companion object {
        fun compressIfImageAndExceedsSizeLimit(byteArray: ByteArray, fileSizeLimit: Int = FILE_SIZE_LIMIT, compressionQuality: Int = DEFAULT_JPEG_COMPRESSION_QUALITY): ByteArray {
            return if (byteArray.size < fileSizeLimit) {
                byteArray
            } else {
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size);

                return if (bitmap != null) {
                    ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, stream)
                        return stream.toByteArray()
                    }
                } else {
                    byteArray
                }
            }
        }
    }
}
