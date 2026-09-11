package net.gini.android.capture

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.document.XmlDocument
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.test.Helpers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented tests for the Uri based import path exposed through
 * [GiniCapture.Internal.createDocumentForImportedUris].
 *
 * Uses real content Uris served by the test FileProvider (see [Helpers.getAssetFileFileContentUri])
 * to cover the pdf, image, multi-page and xml branches with real ContentResolver behavior.
 */
@RunWith(AndroidJUnit4::class)
class GiniCaptureUriImportInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GiniCapture.newInstance(context).build()
    }

    @After
    fun tearDown() {
        GiniCaptureHelperForInstrumentationTests.setGiniCaptureInstance(null)
        listOf(PDF, PDF_WITH_PASSWORD, IMAGE, QR_CODE_IMAGE, XML).forEach {
            try {
                Helpers.deleteAssetFileFromContentUri(it)
            } catch (ignored: Exception) {
            }
        }
    }

    @Test
    fun singlePdfUri_createsPdfDocument_withDataFromTheUri() {
        // Given
        val uri = Helpers.getAssetFileFileContentUri(PDF)
        val expectedBytes = Helpers.loadAsset(PDF)

        // When
        val callback = importUris(listOf(uri))

        // Then
        assertThat(callback.errors).isEmpty()
        assertThat(callback.successes).hasSize(1)
        val document = callback.successes.first()
        assertThat(document).isInstanceOf(PdfDocument::class.java)
        assertThat(document.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(document.uri).isEqualTo(uri)
        assertThat(document.data).isEqualTo(expectedBytes)
    }

    @Test
    fun singleImageUri_createsOnePageImageMultiPageDocument_andSetsMemoryStore() {
        // Given
        val uri = Helpers.getAssetFileFileContentUri(IMAGE)

        // When
        val callback = importUris(listOf(uri))

        // Then
        assertThat(callback.errors).isEmpty()
        assertThat(callback.successes).hasSize(1)
        val document = callback.successes.first()
        assertThat(document).isInstanceOf(ImageMultiPageDocument::class.java)
        val multiPageDocument = document as ImageMultiPageDocument
        assertThat(multiPageDocument.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(multiPageDocument.documents).hasSize(1)
        // The page was compressed and saved to the ImageDiskStore
        assertThat(multiPageDocument.documents.first().uri).isNotNull()
        assertThat(
            GiniCapture.getInstance().internal()
                .imageMultiPageDocumentMemoryStore.multiPageDocument
        ).isEqualTo(multiPageDocument)
    }

    @Test
    fun multipleImageUris_createPagesInInputOrder() {
        // Given two images with different dimensions
        val firstUri = Helpers.getAssetFileFileContentUri(IMAGE)
        val secondUri = Helpers.getAssetFileFileContentUri(QR_CODE_IMAGE)
        val firstDimensions = assetImageDimensions(IMAGE)
        val secondDimensions = assetImageDimensions(QR_CODE_IMAGE)
        assertThat(firstDimensions).isNotEqualTo(secondDimensions)

        // When
        val callback = importUris(listOf(firstUri, secondUri))

        // Then
        assertThat(callback.errors).isEmpty()
        assertThat(callback.successes).hasSize(1)
        val multiPageDocument = callback.successes.first() as ImageMultiPageDocument
        assertThat(multiPageDocument.documents).hasSize(2)
        // Compression preserves dimensions, so pages can be matched to the input order
        assertThat(imageDimensions(multiPageDocument.documents[0].data))
            .isEqualTo(firstDimensions)
        assertThat(imageDimensions(multiPageDocument.documents[1].data))
            .isEqualTo(secondDimensions)
    }

    @Test
    fun singleXmlUri_createsXmlDocument_withDataFromTheUri() {
        // Given
        val uri = Helpers.getAssetFileFileContentUri(XML)
        val expectedBytes = Helpers.loadAsset(XML)

        // When
        val callback = importUris(listOf(uri))

        // Then
        assertThat(callback.errors).isEmpty()
        assertThat(callback.successes).hasSize(1)
        val document = callback.successes.first()
        assertThat(document).isInstanceOf(XmlDocument::class.java)
        assertThat(document.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(document.data).isEqualTo(expectedBytes)
    }

    @Test
    fun passwordProtectedPdfUri_returnsPasswordProtectedPdfError() {
        // Given
        val uri = Helpers.getAssetFileFileContentUri(PDF_WITH_PASSWORD)

        // When
        val callback = importUris(listOf(uri))

        // Then
        assertThat(callback.successes).isEmpty()
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().validationError)
            .isEqualTo(FileImportValidator.Error.PASSWORD_PROTECTED_PDF)
    }

    @Test
    fun multiplePdfUris_returnError_becauseNoImagesWereContained() {
        // Given
        val pdfUri = Helpers.getAssetFileFileContentUri(PDF)

        // When
        val callback = importUris(listOf(pdfUri, pdfUri))

        // Then
        assertThat(callback.successes).isEmpty()
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().message).isEqualTo("Uris did not contain images")
    }

    private fun importUris(uris: List<Uri>): RecordingCallback {
        val callback = RecordingCallback()
        lateinit var token: net.gini.android.capture.util.CancellationToken
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            token = GiniCapture.getInstance().internal()
                .createDocumentForImportedUris(uris, context, callback)
        }
        assertThat(token).isNotNull()
        assertThat(callback.latch.await(30, TimeUnit.SECONDS)).isTrue()
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
        return callback
    }

    private fun assetImageDimensions(assetFileName: String): Pair<Int, Int> =
        imageDimensions(Helpers.loadAsset(assetFileName))

    private fun imageDimensions(bytes: ByteArray?): Pair<Int, Int> {
        assertThat(bytes).isNotNull()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes!!, 0, bytes.size, options)
        return Pair(options.outWidth, options.outHeight)
    }

    private class RecordingCallback :
        AsyncCallback<Document, ImportedFileValidationException> {

        val latch = CountDownLatch(1)
        val successes = mutableListOf<Document>()
        val errors = mutableListOf<ImportedFileValidationException>()
        private var cancelledCount = 0

        val terminalCallbackCount: Int
            get() = successes.size + errors.size + cancelledCount

        override fun onSuccess(result: Document) {
            successes.add(result)
            latch.countDown()
        }

        override fun onError(exception: ImportedFileValidationException) {
            errors.add(exception)
            latch.countDown()
        }

        override fun onCancelled() {
            cancelledCount++
            latch.countDown()
        }
    }

    companion object {
        private const val PDF = "invoice.pdf"
        private const val PDF_WITH_PASSWORD = "invoice-password.pdf"
        private const val IMAGE = "invoice.jpg"
        private const val QR_CODE_IMAGE = "qrcode_bezahlcode.jpeg"
        private const val XML = "invoice.xml"
    }
}
