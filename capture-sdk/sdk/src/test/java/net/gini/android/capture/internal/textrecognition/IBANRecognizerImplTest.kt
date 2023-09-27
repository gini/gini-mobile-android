package net.gini.android.capture.internal.textrecognition

import android.media.Image
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.capture.internal.iban.IBANRecognizer
import net.gini.android.capture.internal.iban.IBANRecognizerImpl
import net.gini.android.capture.test.Helpers.loadJavaResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Some test cases and documents were taken from the extraction service's
 * [IBANExtractor tests](https://github.com/gini/semantics-semantics/blob/42663a59392f56827366fbbbf721cfd637cde641/extractor-commons/src/test/scala/net/gini/semantics/bank_data/extractor/IBANExtractorSpec.scala).
 */
@RunWith(JUnitParamsRunner::class)
class IBANRecognizerImplTest {

    private lateinit var ibanRecognizer: IBANRecognizer

    @Before
    fun setup() {
        ibanRecognizer = IBANRecognizerImpl(TextRecognizerDummy())
    }

    @Test
    fun `returns null when no IBAN found in image byte array`() {
        // Given
        val byteArray = ByteArray(100)
        ibanRecognizer = IBANRecognizerImpl(TextRecognizerStub(""))

        // When
        ibanRecognizer.processByteArray(byteArray, 200, 300, 0) { iban ->
            // Then
            assertThat(iban).isNull()
        }
    }

    @Test
    fun `returns null when no IBAN found in image`() {
        // Given
        val image: Image = mock()
        ibanRecognizer = IBANRecognizerImpl(TextRecognizerStub(""))

        // When
        ibanRecognizer.processImage(image, 200, 300, 0) { iban ->
            // Then
            assertThat(iban).isNull()
        }
    }

    @Test
    fun `throws IllegalArgumentException if image width is 0 in byte array`() {
        // Given
        val byteArray = ByteArray(0)

        // When
        var exception: Exception? = null
        try {
            ibanRecognizer.processByteArray(byteArray, 0, 300, 0) { }
        } catch (e: Exception) {
            exception = e
        }

        // Then
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws IllegalArgumentException if image width is 0`() {
        // Given
        val image: Image = mock()

        // When
        var exception: Exception? = null
        try {
            ibanRecognizer.processImage(image, 0, 300, 0) { }
        } catch (e: Exception) {
            exception = e
        }

        // Then
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws IllegalArgumentException if image height is 0 in byte array`() {
        // Given
        val byteArray = ByteArray(0)

        // When
        var exception: Exception? = null
        try {
            val iban = ibanRecognizer.processByteArray(byteArray, 200, 0, 0) { }
        } catch (e: Exception) {
            exception = e
        }

        // Then
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `throws IllegalArgumentException if image height is 0`() {
        // Given
        val image: Image = mock()

        // When
        var exception: Exception? = null
        try {
            val iban = ibanRecognizer.processImage(image, 200, 0, 0) { }
        } catch (e: Exception) {
            exception = e
        }

        // Then
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @Parameters(method = "recognizesIBANTestParameterValues")
    @Suppress("JUnitMalformedDeclaration")
    fun `recognizes all IBANs in image byte array`(recognizedText: String, expectedIBANs: List<String>) {
        // Given
        val byteArray = ByteArray(100)
        ibanRecognizer = IBANRecognizerImpl(TextRecognizerStub(recognizedText))

        // When
        ibanRecognizer.processByteArray(byteArray, 200, 300, 0) { ibans ->
            // Then
            assertThat(ibans).isEqualTo(expectedIBANs)
        }
    }

    @Test
    @Parameters(method = "recognizesIBANTestParameterValues")
    @Suppress("JUnitMalformedDeclaration")
    fun `recognizes IBAN in image`(recognizedText: String, expectedIBANs: List<String>) {
        // Given
        val image: Image = mock()
        ibanRecognizer = IBANRecognizerImpl(TextRecognizerStub(recognizedText))

        // When
        ibanRecognizer.processImage(image, 200, 300, 0) { ibans ->
            // Then
            assertThat(ibans).isEqualTo(expectedIBANs)
        }
    }

    private fun recognizesIBANTestParameterValues(): Array<Any> = arrayOf(
        // recognizedText, expectedIBANs
        arrayOf("DE78500105172594181438", listOf("DE78500105172594181438")),
        arrayOf("AT195400071341364866", listOf("AT195400071341364866")),
        // support IBANs with whitespace
        arrayOf(loadTextResource("o2doc2-dookuid-8645.txt"), listOf("DE16700202700005713153")),
        // use first detected IBAN
        arrayOf(loadTextResource("samplecontract-dookuid-380.txt"), listOf("DE50700100800014060800", "DE64700202700000088811", "DE23701500000000109850")),
        // prefer German IBANs
        arrayOf(loadTextResource("dookuid-1311.txt"), listOf("DE92680800300672270200")),
        // support IBANs with whitespace
        arrayOf(loadTextResource("smantix_1756-dookuid-281.txt"), listOf("DE28430609672032163700")),
    )

    private fun loadTextResource(name: String): String {
        return loadJavaResource(name).toString(Charsets.UTF_8)
    }

    class TextRecognizerDummy : TextRecognizer {
        override fun processImage(
            image: Image,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (String?) -> Unit
        ) {
            doneCallback(null)
        }

        override fun processByteArray(
            byteArray: ByteArray,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (String?) -> Unit
        ) {
            doneCallback(null)
        }

        override fun close() {

        }

    }

    class TextRecognizerStub(private val text: String?) : TextRecognizer {
        override fun processImage(
            image: Image,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (String?) -> Unit
        ) {
            doneCallback(text)
        }

        override fun processByteArray(
            byteArray: ByteArray,
            width: Int,
            height: Int,
            rotationDegrees: Int,
            doneCallback: (String?) -> Unit
        ) {
            doneCallback(text)
        }

        override fun close() {

        }

    }
}