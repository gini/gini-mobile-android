package net.gini.android.capture.internal.ocr

import android.graphics.Rect
import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Created by Alpár Szotyori on 27.04.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */
class OCR {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var processingTask: Task<Text>? = null

    fun processImage(image: Image, rotation: Int, doneCallback: (OCRText?) -> Unit) {
        if (processingTask != null) return

        processingTask = recognizer.process(InputImage.fromMediaImage(image, rotation))
            .addOnSuccessListener { result ->
                LOG.debug("OCR processing success:")
                val resultText = result.text

                val ocrElements = result.textBlocks.fold(mutableListOf<OCRElement>()) { acc, textBlock ->
                    textBlock.lines.fold(acc) { acc, line ->
                        acc.apply {
                            add(OCRElement(line.text, line.boundingBox ?: Rect()))
                        }
//                        line.elements.fold(acc) { acc, element ->
//                            acc.apply {
//                                add(OCRElement(element.text, element.boundingBox ?: Rect(), element.cornerPoints ?: emptyArray()))
//                            }
//                        }
                    }
                }
                doneCallback(OCRText(ocrElements))
                for (block in result.textBlocks) {
                    val blockText = block.text
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                            LOG.debug("'{}' at {}", elementText, elementFrame)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                LOG.error("OCR processing failed", e)
                doneCallback(null)
            }
            .addOnCompleteListener {
                processingTask = null
            }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OCR::class.java)
    }
}