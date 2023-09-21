package net.gini.android.capture.internal.iban

import android.media.Image
import net.gini.android.capture.internal.textrecognition.MLKitTextRecognizer
import net.gini.android.capture.internal.textrecognition.TextRecognizer
import kotlin.jvm.Throws

/**
 * Use this class to recognize IBANs in images.
 *
 * IBAN extraction logic was written based on the extractor service's
 * [IBANExtractor](https://github.com/gini/semantics-semantics/blob/42663a59392f56827366fbbbf721cfd637cde641/extractor-commons/src/main/scala/net/gini/semantics/bank_data/extractor/IBANExtractor.scala).
 *
 * @param textRecognizer a [TextRecognizer] implementation
 * @param preferredIBANRegex a [Regex] used to find preferred IBANs and return the first one in the callback.
 *                           If there were no preferred IBANs found then the first recognized IBAN will be returned in the callback.
 *                           Default value is a German IBAN regex.
 */
internal class IBANRecognizer @JvmOverloads constructor(
    private val textRecognizer: TextRecognizer,
    private val preferredIBANRegex: Regex = IBANKnowledge.germanIBANRegex
) {

    private val ibanValidator = IBANValidator()

    /**
     * Processes the given [Image] and returns the first recognized IBAN in the callback.
     *
     * If the [preferredIBANRegex] matches IBANs then the first matched one will be returned in the callback.
     *
     * @param image the image to process
     * @param width the width of the image
     * @param height the height of the image
     * @param rotationDegrees the rotation of the image
     * @param doneCallback the callback which will receive the recognized IBAN or null if no IBAN was found
     */
    @Throws(IllegalArgumentException::class)
    fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (String?) -> Unit
    ) {
        checkWidthAndHeight(width, height)
        textRecognizer.processImage(image, width, height, rotationDegrees) { recognizedText ->
            extractIBAN(recognizedText, doneCallback)
        }
    }

    private fun extractIBAN(recognizedText: String?, doneCallback: (String?) -> Unit) {
        if (!recognizedText.isNullOrEmpty()) {
            val ibans = (singleLineIBANs(recognizedText) + ibanInBlocks(recognizedText))
                .removeInvalidIBANs()
                .removeWhitespace()

            val preferredIBANs = ibans.filter(::isPreferredIBAN)

            if (preferredIBANs.isNotEmpty()) {
                doneCallback(preferredIBANs.first())
            } else {
                doneCallback(ibans.firstOrNull())
            }
        } else {
            doneCallback(null)
        }
    }

    private fun isPreferredIBAN(iban: String): Boolean = preferredIBANRegex.matches(iban)

    private fun List<String>.removeInvalidIBANs(): List<String> {
        return this.filter { iban ->
            try {
                ibanValidator.validate(iban)
                true
            } catch (e: IBANValidator.IllegalIBANException) {
                false
            }
        }
    }

    private fun List<String>.removeWhitespace(): List<String> {
        return this.map { it.replace("""\s""".toRegex(), "") }
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
    fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (String?) -> Unit
    ) {
        checkWidthAndHeight(width, height)
        textRecognizer.processByteArray(byteArray, width, height, rotationDegrees) { recognizedText ->
            extractIBAN(recognizedText, doneCallback)
        }
    }

    private fun checkWidthAndHeight(width: Int, height: Int) {
        if (width == 0) {
            throw IllegalArgumentException("Image width is 0")
        }
        if (height == 0) {
            throw IllegalArgumentException("Image height is 0")
        }
    }

    private fun singleLineIBANs(text: String): List<String> {
        return IBANKnowledge.universalIBANRegex.findAll(text)
            .map { it.value }.toList()
    }

    private fun ibanInBlocks(text: String): List<String> {
        return IBANKnowledge.ibanInBlocksRegex.findAll(text)
            .map { it.value }.toList()
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
