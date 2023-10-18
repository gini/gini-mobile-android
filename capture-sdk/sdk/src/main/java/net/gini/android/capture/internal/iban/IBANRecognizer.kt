package net.gini.android.capture.internal.iban

import android.media.Image

/**
 * Interface for processing images and returning the recognized IBANs in the callback.
 */
interface IBANRecognizer {

    /**
     * Processes the given [Image] and returns the recognized IBANs in the callback.
     *
     * @param image the image to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized IBANs or null if no IBAN was found
     * @param cancelledCallback the callback which will be called when the processing is cancelled
     */
    @Throws(IllegalArgumentException::class)
    fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (List<String>) -> Unit,
        cancelledCallback: () -> Unit
    )

    /**
     * Processes the given image byte array and returns the recognized IBANs in the callback.
     *
     * @param byteArray the image byte array to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized IBANs or null if no IBAN was found
     * @param cancelledCallback the callback which will be called when the processing is cancelled
     */
    @Throws(IllegalArgumentException::class)
    fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (List<String>) -> Unit,
        cancelledCallback: () -> Unit
    )

    /**
     * Closes the IBAN recognizer.
     *
     * **IMPORTANT**: You must call this method when you are done with the IBAN recognizer.
     */
    fun close()
}