package net.gini.android.capture.internal.textrecognition.test

import android.media.Image
import net.gini.android.capture.internal.textrecognition.RecognizedText
import net.gini.android.capture.internal.textrecognition.TextRecognizer

internal class TextRecognizerStub(
    private val text: RecognizedText?,
    private val isCancelled: Boolean = false) : TextRecognizer {

    override fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (isCancelled) {
            cancelledCallback()
        } else {
            doneCallback(text)
        }
    }

    override fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (isCancelled) {
            cancelledCallback()
        } else {
        doneCallback(text)
    }
    }

    override fun close() {

    }
}