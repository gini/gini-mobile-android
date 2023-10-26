package net.gini.android.capture.internal.textrecognition.test

import android.media.Image
import net.gini.android.capture.internal.textrecognition.RecognizedText
import net.gini.android.capture.internal.textrecognition.TextRecognizer

internal class TextRecognizerDummy : TextRecognizer {
    override fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        doneCallback(null)
    }

    override fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        doneCallback(null)
    }

    override fun close() {

    }
}