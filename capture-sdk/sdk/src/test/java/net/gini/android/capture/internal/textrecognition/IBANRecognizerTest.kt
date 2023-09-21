package net.gini.android.capture.internal.textrecognition

import android.media.Image
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.capture.internal.iban.IBANRecognizer
import net.gini.android.capture.test.Helpers.loadJavaResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Some test cases and documents were taken from the extraction service's
 * [IBANExtractor tests](https://github.com/gini/semantics-semantics/blob/42663a59392f56827366fbbbf721cfd637cde641/extractor-commons/src/test/scala/net/gini/semantics/bank_data/extractor/IBANExtractorSpec.scala).
 */
@RunWith(JUnitParamsRunner::class)
class IBANRecognizerTest {

    private lateinit var ibanRecognizer: IBANRecognizer

    @Before
    fun setup() {
        ibanRecognizer = IBANRecognizer(TextRecognizerDummy())
    }

    @Test
    fun `returns null when no IBAN found in image byte array`() {
        // Given
        val byteArray = ByteArray(100)
        ibanRecognizer = IBANRecognizer(TextRecognizerStub(""))

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
        ibanRecognizer = IBANRecognizer(TextRecognizerStub(""))

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
    fun `recognizes IBAN in image byte array`(recognizedText: String, expectedIBAN: String) {
        // Given
        val byteArray = ByteArray(100)
        ibanRecognizer = IBANRecognizer(TextRecognizerStub(recognizedText))

        // When
        ibanRecognizer.processByteArray(byteArray, 200, 300, 0) { iban ->
            // Then
            assertThat(iban).isEqualTo(expectedIBAN)
        }
    }

    @Test
    @Parameters(method = "recognizesIBANTestParameterValues")
    @Suppress("JUnitMalformedDeclaration")
    fun `recognizes IBAN in image`(recognizedText: String, expectedIBAN: String) {
        // Given
        val image: Image = mock()
        ibanRecognizer = IBANRecognizer(TextRecognizerStub(recognizedText))

        // When
        ibanRecognizer.processImage(image, 200, 300, 0) { iban ->
            // Then
            assertThat(iban).isEqualTo(expectedIBAN)
        }
    }

    private fun recognizesIBANTestParameterValues(): Array<Any> = arrayOf(
        // recognizedText, expectedIBAN
        arrayOf("DE78500105172594181438", "DE78500105172594181438"),
        arrayOf("AT195400071341364866", "AT195400071341364866"),
        // support IBANs with whitespace
        arrayOf(loadTextResource("o2doc2-dookuid-8645.txt"), "DE16700202700005713153"),
        // use first detected IBAN
        arrayOf(loadTextResource("samplecontract-dookuid-380.txt"), "DE64700202700000088811"),
        // prefer German IBANs
        arrayOf(loadTextResource("dookuid-1311.txt"), "DE92680800300672270200"),
        // support IBANs with whitespace
        arrayOf(loadTextResource("smantix_1756-dookuid-281.txt"), "DE28430609672032163700"),
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