package net.gini.android.capture.internal.textrecognition

import android.media.Image
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    @Parameters(method = "recognizeIBANinImageValues")
    @Suppress("JUnitMalformedDeclaration")
    fun `recognize IBAN in image byte array`(recognizedText: String, expectedIBAN: String) {
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
    @Parameters(method = "recognizeIBANinImageValues")
    @Suppress("JUnitMalformedDeclaration")
    fun `recognize IBAN in image`(recognizedText: String, expectedIBAN: String) {
        // Given
        val image: Image = mock()
        ibanRecognizer = IBANRecognizer(TextRecognizerStub(recognizedText))

        // When
        ibanRecognizer.processImage(image, 200, 300, 0) { iban ->
            // Then
            assertThat(iban).isEqualTo(expectedIBAN)
        }
    }

    private fun recognizeIBANinImageValues(): Array<Any> = arrayOf(
        // recognizedText, expectedIBAN
        arrayOf("DE78500105172594181438", "DE78500105172594181438"),
        arrayOf("DE78 5001 0517 2594 1814 38", "DE78500105172594181438")
        // TODO: Add more values
    )

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