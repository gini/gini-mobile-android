package net.gini.android.capture.internal.textrecognition

import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger

/**
 * Use this class to recognize text in images via the ML Kit Text Recognition API.
 */
internal class MLKitTextRecognizer(private val recognizer: com.google.mlkit.vision.text.TextRecognizer) :
    TextRecognizer {

    private var processingTask: Task<Text>? = null

    /**
     * Processes the given [Image] and returns the recognized text in the callback.
     *
     * **IMPORTANT**: If an image is already processing then `cancelledCallback` will be called.
     *
     * @param image the image to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized text or null if no text was found
     */
    override fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (processingTask != null) {
            LOG.warn("Text recognizer is already processing an image")
            cancelledCallback()
            return
        }

        processingTask = recognizer.process(InputImage.fromMediaImage(image, rotationDegrees))
        handleProcessingTask(doneCallback)
    }

    /**
     * Processes the given image byte array and returns the recognized text in the callback.
     *
     * **IMPORTANT**: If an image is already processing then `cancelledCallback` will be called.
     *
     * @param byteArray the image byte array to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized text or null if no text was found
     */
    override fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (RecognizedText?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (processingTask != null) {
            LOG.warn("Text recognizer is already processing an image")
            cancelledCallback()
            return
        }

        processingTask = recognizer.process(
            InputImage.fromByteArray(
                byteArray,
                width,
                height,
                rotationDegrees,
                InputImage.IMAGE_FORMAT_NV21
            )
        )
        handleProcessingTask(doneCallback)
    }

    override fun close() {
        recognizer.close()
    }

    private fun handleProcessingTask(doneCallback: (RecognizedText?) -> Unit) {
        processingTask
            ?.addOnSuccessListener { result ->
                LOG.debug("Text recognizer success: {}", result.text)
                doneCallback(mlKitTextToRecognizedText(result))
            }
            ?.addOnFailureListener { e ->
                LOG.error("Text recognizer failed", e)
                doneCallback(null)
            }
            ?.addOnCompleteListener {
                processingTask = null
            }
    }

    private fun mlKitTextToRecognizedText(mlKitText: Text): RecognizedText {
        return RecognizedText(mlKitText.text, mlKitText.textBlocks.map { block ->
            RecognizedTextBlock(block.lines.map { line ->
                RecognizedTextLine(line.elements.map { element ->
                    RecognizedTextElement(element.text, element.boundingBox)
                })
            })
        })
    }

    companion object {
        private const val DEBUG = false
        val LOG: Logger = if (DEBUG) LoggerFactory.getLogger(MLKitTextRecognizer::class.java) else NOPLogger.NOP_LOGGER

        @JvmStatic
        fun newInstance() = MLKitTextRecognizer(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
    }
}
