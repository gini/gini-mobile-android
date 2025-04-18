package net.gini.android.capture.internal.iban

import android.media.Image
import net.gini.android.capture.internal.textrecognition.TextRecognizer

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
internal class IBANRecognizerImpl @JvmOverloads constructor(
    private val textRecognizer: TextRecognizer,
    private val preferredIBANRegex: Regex = IBANKnowledge.germanIBANRegex
): IBANRecognizer {

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
    override fun processImage(
        image: Image,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (List<String>) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        checkWidthAndHeight(width, height)
        textRecognizer.processImage(image, width, height, rotationDegrees, doneCallback = { recognizedText ->
            extractIBAN(recognizedText?.text, doneCallback)
        }, cancelledCallback = {
            cancelledCallback()
        })
    }

    private fun extractIBAN(recognizedText: String?, doneCallback: (List<String>) -> Unit) {
        if (!recognizedText.isNullOrEmpty()) {
            val ibansFromRegex = (singleLineIBANs(recognizedText) + ibanInBlocks(recognizedText))
                .removeWhitespace()
                .distinct()

            var ibans = ibansFromRegex
                .removeInvalidIBANs()

            if (ibans.isEmpty()) {
                ibans = ibansFromRegex.map { iban ->
                    iban.substring(0, 2) + iban.substring(2).map { char ->
                        when (char) {
                            'S', 's' -> '5'
                            'B' -> '8'
                            'Z' -> '7'
                            'I', 'i', 'l' ,'T' -> '1'
                            'O', 'o', 'Q' -> '0'
                            else -> char
                        }
                    }.joinToString("")
                }.removeInvalidIBANs()
            }

            val preferredIBANs = ibans.filter(::isPreferredIBAN)

            if (preferredIBANs.isNotEmpty()) {
                doneCallback(preferredIBANs)
            } else {
                doneCallback(ibans)
            }
        } else {
            doneCallback(emptyList())
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
    override fun processByteArray(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        doneCallback: (List<String>) -> Unit,
        cancelledCallback: () -> Unit
    ) {
        checkWidthAndHeight(width, height)
        textRecognizer.processByteArray(byteArray, width, height, rotationDegrees, doneCallback = { recognizedText ->
            extractIBAN(recognizedText?.text, doneCallback)
        }, cancelledCallback = {
            cancelledCallback()
        })
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
    override fun close() {
        textRecognizer.close()
    }
}
