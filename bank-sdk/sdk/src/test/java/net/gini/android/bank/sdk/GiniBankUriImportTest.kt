package net.gini.android.bank.sdk

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for the Uri based capture entry points of [GiniBank]:
 * [GiniBank.createCaptureFlowFragmentForUris] and the List<Uri> overload of
 * [GiniBank.createDocumentForImportedFiles].
 */
// sdk 33: on newer emulated SDKs PdfRenderer delegates to PdfProcessor, which throws
// NoSuchMethodError under Robolectric and cannot be caught by FileImportValidator
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GiniBankUriImportTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton())
            .addExtensionMimeTypeMapping("pdf", "application/pdf")
    }

    @After
    fun tearDown() {
        GiniBank.cleanupCapture(context)
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test(expected = IllegalStateException::class)
    fun `createCaptureFlowFragmentForUris throws IllegalStateException when capture is not configured`() {
        GiniBank.createCaptureFlowFragmentForUris(context, listOf(createPdfUri())) {}
    }

    @Test
    fun `createDocumentForImportedFiles with uris returns null without invoking the callback when capture is not configured`() {
        // Given
        var callbackInvoked = false

        // When
        val token = GiniBank.createDocumentForImportedFiles(listOf(createPdfUri()), context) {
            callbackInvoked = true
        }

        // Then
        assertThat(token).isNull()
        assertThat(callbackInvoked).isFalse()
    }

    @Test
    fun `createCaptureFlowFragmentForUris returns Success with a CaptureFlowFragment for a pdf Uri`() {
        // Given
        configureCapture()
        val uri = createPdfUri()
        val result = AtomicReference<GiniBank.CreateCaptureFlowFragmentForIntentResult>()

        // When
        val token = GiniBank.createCaptureFlowFragmentForUris(context, listOf(uri)) {
            result.set(it)
        }
        awaitResult(result)

        // Then
        assertThat(token).isNotNull()
        val success = result.get()
        assertThat(success)
            .isInstanceOf(GiniBank.CreateCaptureFlowFragmentForIntentResult.Success::class.java)
        assertThat((success as GiniBank.CreateCaptureFlowFragmentForIntentResult.Success).fragment)
            .isNotNull()
    }

    @Test
    fun `document created by createDocumentForImportedFiles with uris is accepted by createCaptureFlowFragmentForDocument`() {
        // Given
        configureCapture()
        val pdfBytes = pdfBytes()
        val uri = createPdfUri(pdfBytes)
        val result = AtomicReference<GiniBank.CreateDocumentFromImportedFileResult>()

        // When
        val token = GiniBank.createDocumentForImportedFiles(listOf(uri), context) {
            result.set(it)
        }
        awaitResult(result)

        // Then
        assertThat(token).isNotNull()
        val success = result.get()
        assertThat(success)
            .isInstanceOf(GiniBank.CreateDocumentFromImportedFileResult.Success::class.java)
        val document = (success as GiniBank.CreateDocumentFromImportedFileResult.Success).document
        assertThat(document).isInstanceOf(PdfDocument::class.java)
        assertThat(document!!.data).isEqualTo(pdfBytes)
        assertThat(GiniBank.createCaptureFlowFragmentForDocument(document)).isNotNull()
    }

    @Test
    fun `createCaptureFlowFragmentForUris returns Error for an empty Uri list`() {
        // Given
        configureCapture()
        val result = AtomicReference<GiniBank.CreateCaptureFlowFragmentForIntentResult>()

        // When
        GiniBank.createCaptureFlowFragmentForUris(context, emptyList()) {
            result.set(it)
        }
        awaitResult(result)

        // Then
        val error = result.get()
        assertThat(error)
            .isInstanceOf(GiniBank.CreateCaptureFlowFragmentForIntentResult.Error::class.java)
        val exception = (error as GiniBank.CreateCaptureFlowFragmentForIntentResult.Error).exception
        assertThat(exception.message).isEqualTo("Uri list is empty")
        assertThat(exception.validationError).isNull()
    }

    private fun configureCapture() {
        val mockNetworkService = mockk<GiniCaptureNetworkService>(relaxed = true)
        GiniBank.setCaptureConfiguration(context, CaptureConfiguration(networkService = mockNetworkService))
    }

    private fun pdfBytes(): ByteArray = "%PDF-1.4 test pdf content".toByteArray()

    private fun createPdfUri(bytes: ByteArray = pdfBytes()): Uri {
        val file = File.createTempFile("gini-bank-uri-import-test", ".pdf", context.cacheDir)
        file.writeBytes(bytes)
        tempFiles.add(file)
        return Uri.fromFile(file)
    }

    private fun <T> awaitResult(result: AtomicReference<T>, timeoutMillis: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (result.get() == null && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertThat(result.get()).isNotNull()
    }
}
