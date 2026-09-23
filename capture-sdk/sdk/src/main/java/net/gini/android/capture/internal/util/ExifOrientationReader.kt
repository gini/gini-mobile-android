package net.gini.android.capture.internal.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Internal use only.
 *
 * Reads the EXIF orientation of an image with the platform's own reader.
 *
 * The SDK normally takes the rotation of an imported image from
 * [net.gini.android.capture.internal.camera.photo.ExifReader], which is built
 * on Apache Commons Imaging and only understands JPEG. A HEIC therefore
 * arrives with no rotation at all, and a portrait photo would be uploaded
 * lying on its side. The platform reader handles HEIF from Android 9, which is
 * the same version that can decode HEIC in the first place.
 *
 * @suppress
 */
object ExifOrientationReader {

    private val LOG = LoggerFactory.getLogger(ExifOrientationReader::class.java)

    private const val QUARTER_TURN = 90
    private const val HALF_TURN = 180
    private const val THREE_QUARTER_TURN = 270

    /**
     * Clockwise degrees the image at [uri] has to be rotated by to be shown
     * the right way up, or `0` when there is no usable orientation tag.
     */
    @JvmStatic
    fun readRotationForDisplay(uri: Uri, context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // ExifInterface cannot read from a stream before Android 7, and
            // HEIC is not decodable before Android 9 anyway.
            return 0
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                toRotationForDisplay(
                    ExifInterface(inputStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                )
            } ?: 0
        } catch (e: IOException) {
            LOG.warn("Could not read exif orientation of uri {}", LogSanitizer.sanitize(uri), e)
            0
        } catch (e: SecurityException) {
            LOG.warn("Not permitted to read uri {}", LogSanitizer.sanitize(uri), e)
            0
        }
    }

    /**
     * Maps an EXIF orientation value to clockwise degrees, matching
     * [net.gini.android.capture.internal.camera.photo.ExifReader] so that a
     * HEIC and a JPEG of the same photo end up rotated identically. Mirrored
     * orientations are treated as their unmirrored equivalent, as they are on
     * the JPEG path.
     */
    @JvmStatic
    fun toRotationForDisplay(exifOrientation: Int): Int = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
        ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
        ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
        else -> 0
    }
}
