package net.gini.android.capture.internal.textrecognition

import android.media.Image

/**
 * Interface for recognizing text in images.
 */
internal interface TextRecognizer {

    /**
     * Processes the given [Image] and returns the recognized text in the callback.
     *
     * @param image the image to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized text or null if no text was found
     * @param cancelledCallback the callback which will be called when the processing is cancelled
     */
    fun processImage(image: Image,
                     width: Int,
                     height: Int,
                     rotationDegrees: Int,
                     doneCallback: (RecognizedText?) -> Unit,
                     cancelledCallback: () -> Unit)

    /**
     * Processes the given image byte array and returns the recognized text in the callback.
     *
     * @param byteArray the image byte array to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized text or null if no text was found
     * @param cancelledCallback the callback which will be called when the processing is cancelled
     */
    fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit)

    /**
     * Closes the text recognizer.
     *
     * **IMPORTANT**: You must call this method when you are done with the text recognizer.
     */
    fun close()
}