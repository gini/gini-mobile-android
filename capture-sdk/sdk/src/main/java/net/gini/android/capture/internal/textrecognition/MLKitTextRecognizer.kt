package net.gini.android.capture.internal.textrecognition

import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Use this class to recognize text in images via the ML Kit Text Recognition API.
 */
internal class MLKitTextRecognizer(private val recognizer: com.google.mlkit.vision.text.TextRecognizer) :
    TextRecognizer {

    private var processingTask: Task<Text>? = null

    /**
     * Processes the given [Image] and returns the recognized text in the callback.
     *
     * **IMPORTANT**: If an image is already processing then `doneCallback` will be called with null and the new image
     * will not be processed.
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
        doneCallback: (String?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (processingTask != null) {
            if (DEBUG) {
                LOG.warn("Text recognizer is already processing an image")
            }
            cancelledCallback()
            return
        }

        processingTask = recognizer.process(InputImage.fromMediaImage(image, rotationDegrees))
        handleProcessingTask(doneCallback)
    }

    /**
     * Processes the given image byte array and returns the recognized text in the callback.
     *
     * **IMPORTANT**: If an image is already processing then `doneCallback` will be called with null and the new image
     * will not be processed.
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
        doneCallback: (String?) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        if (processingTask != null) {
            if (DEBUG) {
                LOG.warn("Text recognizer is already processing an image")
            }
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

    private fun handleProcessingTask(doneCallback: (String?) -> Unit) {
        processingTask
            ?.addOnSuccessListener { result ->
                if (DEBUG) {
                    LOG.debug("Text recognizer success: {}", result.text)
                }
                doneCallback(result.text)
            }
            ?.addOnFailureListener { e ->
                if (DEBUG) {
                    LOG.error("Text recognizer failed", e)
                }
                doneCallback(null)
            }
            ?.addOnCompleteListener {
                processingTask = null
            }
    }

    companion object {
        const val DEBUG = true
        val LOG: Logger = LoggerFactory.getLogger(MLKitTextRecognizer::class.java)

        @JvmStatic
        fun newInstance() = MLKitTextRecognizer(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
    }
}