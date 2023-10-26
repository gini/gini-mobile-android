package net.gini.android.capture.internal.textrecognition

import android.graphics.Rect
import android.media.Image
import net.gini.android.capture.internal.util.Size

/**
 * Text recognizer which crops the recognized text from an actual text recognizer to the camera frame.
 */
internal class CropToCameraFrameTextRecognizer(private val actualTextRecognizer: TextRecognizer): TextRecognizer {

    var cameraPreviewSize: Size = Size(1, 1)
        /**
         * Set the camera preview size to be used for cropping the recognized text.
         */
        set(value) {
            field = value
            scaleX = cameraPreviewSize.width.toFloat() / imageSize.width
            scaleY = cameraPreviewSize.height.toFloat() / imageSize.height
        }

    /**
     * Set the camera frame's rectangle to be used for cropping the recognized text.
     */
    var cameraFrameRect: Rect? = null

    private var imageSize: Size = Size(1, 1)
        set(value) {
            field = value
            scaleX = cameraPreviewSize.width.toFloat() / imageSize.width
            scaleY = cameraPreviewSize.height.toFloat() / imageSize.height
        }
    private var imageRotation = 0
    private var scaleX = 1f
    private var scaleY = 1f

    /**
     * Sets the image size and rotation to be used for cropping the recognized text.
     */
    fun setImageSizeAndRotation(size: Size, rotation: Int) {
        imageRotation = rotation % 360
        imageSize = when (imageRotation) {
            90, 270 -> Size(size.height, size.width)
            else -> size
        }
    }

    override fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        actualTextRecognizer.processImage(image, width, height, rotationDegrees, doneCallback = { recognizedText ->
            if (recognizedText == null) {
                doneCallback(null)
            } else {
                doneCallback(getCroppedRecognizedText(recognizedText))
            }
        }, cancelledCallback)
    }

    override fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        actualTextRecognizer.processByteArray(byteArray, width, height, rotationDegrees, doneCallback = { recognizedText ->
            if (recognizedText == null) {
                doneCallback(null)
            } else {
                doneCallback(getCroppedRecognizedText(recognizedText))
            }
        }, cancelledCallback)
    }

    /**
     * Crops the recognized text to the camera frame.
     *
     * @param recognizedText the recognized text
     */
    private fun getCroppedRecognizedText(recognizedText: RecognizedText): RecognizedText {
        return cameraFrameRect?.let { frameRect ->
            val croppedBlocks = getCroppedBlocks(recognizedText, frameRect)
            val croppedTextString = getCroppedTextString(recognizedText, frameRect)
            RecognizedText(croppedTextString, croppedBlocks)
        } ?: recognizedText
    }

    /**
     * Crops the recognized text to the camera frame and concatenates the remaining text into a single string.
     *
     * @param recognizedText the recognized text
     * @param cameraFrameRect the camera frame
     */
    private fun getCroppedTextString(
        recognizedText: RecognizedText,
        cameraFrameRect: Rect
    ): String = recognizedText.blocks.fold(StringBuilder()) { acc, textBlock ->
        textBlock.lines.fold(acc) { acc, line ->
            line.elements.fold(acc) { acc, element ->
                if (element.boundingBox != null) {
                    if (cameraFrameRect.contains(getScaledBoundingBox(element.boundingBox))) {
                        acc.append(element.text)
                    }
                }
                acc
            }
            acc.append("\n")
        }
        acc.append("\n")
    }.toString()

    /**
     * Crops the recognized text to the camera frame and returns the cropped text blocks.
     *
     * @param recognizedText the recognized text
     * @param cameraFrameRect the camera frame
     */
    private fun getCroppedBlocks(
        recognizedText: RecognizedText,
        cameraFrameRect: Rect
    ) = recognizedText.blocks.mapNotNull { block ->
        val croppedLines = block.lines.mapNotNull { lines ->
            val croppedElements = lines.elements.filter { element ->
                if (element.boundingBox != null) {
                    cameraFrameRect.contains(getScaledBoundingBox(element.boundingBox))
                } else {
                    false
                }
            }
            if (croppedElements.isNotEmpty()) {
                RecognizedTextLine(croppedElements)
            } else {
                null
            }
        }
        if (croppedLines.isNotEmpty()) {
            RecognizedTextBlock(croppedLines)
        } else {
            null
        }
    }

    private fun getScaledBoundingBox(boundingBox: Rect) = Rect(
        (boundingBox.left * scaleX).toInt(),
        (boundingBox.top * scaleY).toInt(),
        (boundingBox.right * scaleX).toInt(),
        (boundingBox.bottom * scaleY).toInt()
    )

    override fun close() {
        actualTextRecognizer.close()
    }


}