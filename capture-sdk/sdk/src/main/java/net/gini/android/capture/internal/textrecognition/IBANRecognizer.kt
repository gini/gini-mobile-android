package net.gini.android.capture.internal.textrecognition

import android.media.Image
import kotlin.jvm.Throws

/**
 * Use this class to recognize IBANs in images.
 *
 * @param textRecognizer a [TextRecognizer] implementation
 */
internal class IBANRecognizer(private val textRecognizer: TextRecognizer) {

    /**
     * Processes the given [Image] and returns the recognized IBAN in the callback.
     *
     * @param image the image to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized IBAN or null if no IBAN was found
     */
    @Throws(IllegalArgumentException::class)
    fun processImage(image: Image, width: Int, height: Int, rotationDegrees: Int, doneCallback: (String?) -> Unit) {
        if (width == 0) {
            throw IllegalArgumentException("Image width is 0")
        }
        if (height == 0) {
            throw IllegalArgumentException("Image height is 0")
        }
        textRecognizer.processImage(image, width, height, rotationDegrees) { recognizedText ->
            val withoutWhitespace = recognizedText?.replace("\\s".toRegex(), "")
            // TODO: Replace with IBAN recognition logic
            val result = if (!withoutWhitespace.isNullOrEmpty()) {
                "DE78500105172594181438"
            } else {
                null
            }
            doneCallback(result)
        }
    }

    /**
     * Processes the given image byte array and returns the recognized IBAN in the callback.
     *
     * @param byteArray the image byte array to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized IBAN or null if no IBAN was found
     */
    @Throws(IllegalArgumentException::class)
    fun processByteArray(byteArray: ByteArray, width: Int, height: Int, rotationDegrees: Int, doneCallback: (String?) -> Unit) {
        if (width == 0) {
            throw IllegalArgumentException("Image width is 0")
        }
        if (height == 0) {
            throw IllegalArgumentException("Image height is 0")
        }
        textRecognizer.processByteArray(byteArray, width, height, rotationDegrees) { recognizedText ->
            val withoutWhitespace = recognizedText?.replace("\\s".toRegex(), "")
            val result = if (!withoutWhitespace.isNullOrEmpty()) {
                "DE78500105172594181438"
            } else {
                null
            }
            doneCallback(result)
        }
    }

    /**
     * Closes the IBAN recognizer.
     *
     * **IMPORTANT**: You must call this method when you are done with the IBAN recognizer.
     */
    fun close() {
        textRecognizer.close()
    }
}
