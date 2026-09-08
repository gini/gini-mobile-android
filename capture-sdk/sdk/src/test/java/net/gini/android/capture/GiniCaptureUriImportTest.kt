package net.gini.android.capture

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * Unit tests for [GiniCaptureUriImport].
 *
 * Covers the error paths and the pdf, xml, image and multi-image branches using file Uris
 * pointing to real temporary files ([net.gini.android.capture.util.UriHelper] falls back to the
 * file behind the Uri's path for mime type, file size and input stream). The same branches are
 * exercised with real content Uris in GiniCaptureUriImportInstrumentedTest.
 */
// sdk 33: on newer emulated SDKs PdfRenderer delegates to PdfProcessor, which throws
// NoSuchMethodError under Robolectric and cannot be caught by FileImportValidator
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GiniCaptureUriImportTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping("pdf", "application/pdf")
            addExtensionMimeTypeMapping("xml", "text/xml")
            addExtensionMimeTypeMapping("jpg", "image/jpeg")
            addExtensionMimeTypeMapping("jpeg", "image/jpeg")
        }
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun `calls onError when GiniCapture instance is not available`() {
        // Given
        val giniCapture = buildGiniCapture()
        GiniCaptureHelper.setGiniCaptureInstance(null)
        val callback = RecordingCallback()

        // When
        val token = GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(createPdfUri()), context, callback)

        // Then
        assertThat(token).isNotNull()
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().message)
            .contains("GiniCapture instance not available")
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `calls onError when the Uri list is empty`() {
        // Given
        val giniCapture = buildGiniCapture()
        val callback = RecordingCallback()

        // When
        val token = GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(emptyList(), context, callback)

        // Then
        assertThat(token).isNotNull()
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().message).isEqualTo("Uri list is empty")
        assertThat(callback.errors.first().validationError).isNull()
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `creates a PdfDocument with the data behind a single pdf Uri`() {
        // Given
        val giniCapture = buildGiniCapture()
        val pdfBytes = pdfBytes()
        val uri = createPdfUri(pdfBytes)
        val callback = RecordingCallback()

        // When
        val token = GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(token).isNotNull()
        assertThat(callback.errors).isEmpty()
        assertThat(callback.successes).hasSize(1)
        val document = callback.successes.first()
        assertThat(document).isInstanceOf(PdfDocument::class.java)
        assertThat(document.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(document.uri).isEqualTo(uri)
        assertThat(document.data).isEqualTo(pdfBytes)
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `calls onError when the input stream is not available for the Uri`() {
        // Given
        val giniCapture = buildGiniCapture()
        val nonExistentUri =
            Uri.fromFile(File(context.cacheDir, "does-not-exist.pdf"))
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(nonExistentUri), context, callback)

        // Then
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().message)
            .isEqualTo("InputStream not available for the Uri")
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `calls onError with the validation error when the file fails validation`() {
        // Given
        val fileSizeLimit = 1024
        val giniCapture = buildGiniCapture(importedFileSizeBytesLimit = fileSizeLimit)
        val uri = createPdfUri(pdfBytes(paddedSize = fileSizeLimit + 1))
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)

        // Then
        assertThat(callback.errors).hasSize(1)
        assertThat(callback.errors.first().validationError)
            .isEqualTo(FileImportValidator.Error.SIZE_TOO_LARGE)
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `returns a CancellationToken immediately and delivers exactly one terminal callback`() {
        // Given
        val giniCapture = buildGiniCapture()
        val uri = createPdfUri()
        val callback = RecordingCallback()

        // When
        val token = GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)

        // Then
        assertThat(token).isNotNull()
        assertThat(callback.terminalCallbackCount).isEqualTo(0)
        awaitTerminalCallback(callback)
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `creates an XmlDocument with the data behind a single xml Uri`() {
        // Given
        val giniCapture = buildGiniCapture()
        val xmlBytes = "<invoice/>".toByteArray()
        val uri = createUri(xmlBytes, ".xml")
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(callback.errors).isEmpty()
        val document = callback.successes.single()
        assertThat(document).isInstanceOf(XmlDocument::class.java)
        assertThat(document.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(document.uri).isEqualTo(uri)
        assertThat(document.data).isEqualTo(xmlBytes)
    }

    @Test
    fun `creates a one page ImageMultiPageDocument for a single image Uri and stores it in memory`() {
        // Given
        val giniCapture = buildGiniCapture()
        val uri = createUri(Helpers.getTestJpeg(), ".jpg")
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(callback.errors).isEmpty()
        val document = callback.successes.single()
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
    fun `creates one page per image Uri when multiple image Uris are imported`() {
        // Given
        val giniCapture = buildGiniCapture()
        val uris = listOf(
            createUri(Helpers.getTestJpeg(), ".jpg"),
            createUri(Helpers.loadAsset("invoice-valid-user-comment.jpeg"), ".jpeg")
        )
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(uris, context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(callback.errors).isEmpty()
        val multiPageDocument = callback.successes.single() as ImageMultiPageDocument
        assertThat(multiPageDocument.documents).hasSize(2)
        assertThat(multiPageDocument.documents.map { it.uri }).containsNoDuplicates()
    }

    @Test
    fun `calls onError when multiple Uris contain no images`() {
        // Given
        val giniCapture = buildGiniCapture()
        val pdfUri = createPdfUri()
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(pdfUri, pdfUri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(callback.successes).isEmpty()
        assertThat(callback.errors.single().message).isEqualTo("Uris did not contain images")
        assertThat(callback.errors.single().validationError).isNull()
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `calls onError with the validation error when an image Uri fails validation`() {
        // Given
        val fileSizeLimit = 1024
        val giniCapture = buildGiniCapture(importedFileSizeBytesLimit = fileSizeLimit)
        val jpegBytes = Helpers.getTestJpeg()
        assertThat(jpegBytes.size).isGreaterThan(fileSizeLimit)
        val uri = createUri(jpegBytes, ".jpg")
        val callback = RecordingCallback()

        // When
        GiniCaptureUriImport(giniCapture)
            .createDocumentForImportedUris(listOf(uri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(callback.successes).isEmpty()
        assertThat(callback.errors.single().validationError)
            .isEqualTo(FileImportValidator.Error.SIZE_TOO_LARGE)
        assertThat(callback.terminalCallbackCount).isEqualTo(1)
    }

    @Test
    fun `GiniCapture Internal delegates to the Uri import`() {
        // Given
        val giniCapture = buildGiniCapture()
        val pdfBytes = pdfBytes()
        val uri = createPdfUri(pdfBytes)
        val callback = RecordingCallback()

        // When
        val token = giniCapture.internal()
            .createDocumentForImportedUris(listOf(uri), context, callback)
        awaitTerminalCallback(callback)

        // Then
        assertThat(token).isNotNull()
        assertThat(callback.errors).isEmpty()
        val document = callback.successes.single()
        assertThat(document).isInstanceOf(PdfDocument::class.java)
        assertThat(document.data).isEqualTo(pdfBytes)
    }

    private fun buildGiniCapture(importedFileSizeBytesLimit: Int? = null): GiniCapture {
        val builder = GiniCapture.newInstance(context)
        importedFileSizeBytesLimit?.let { builder.setImportedFileSizeBytesLimit(it) }
        builder.build()
        return GiniCapture.getInstance()
    }

    private fun pdfBytes(paddedSize: Int = 0): ByteArray {
        val header = "%PDF-1.4 test pdf content".toByteArray()
        return if (paddedSize > header.size) {
            header + ByteArray(paddedSize - header.size)
        } else {
            header
        }
    }

    private fun createPdfUri(bytes: ByteArray = pdfBytes()): Uri = createUri(bytes, ".pdf")

    private fun createUri(bytes: ByteArray, extension: String): Uri {
        val file = File.createTempFile("gini-uri-import-test", extension, context.cacheDir)
        file.writeBytes(bytes)
        tempFiles.add(file)
        return Uri.fromFile(file)
    }

    private fun awaitTerminalCallback(callback: RecordingCallback, timeoutMillis: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (callback.latch.count > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertThat(callback.latch.count).isEqualTo(0)
    }

    private class RecordingCallback :
        AsyncCallback<Document, ImportedFileValidationException> {

        val latch = CountDownLatch(1)
        val successes = mutableListOf<Document>()
        val errors = mutableListOf<ImportedFileValidationException>()
        var cancelledCount = 0
            private set

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
}
