package net.gini.android.capture.document

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.test.Helpers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [ImageDocument].
 *
 * Covers the Intent-less [ImageDocument.fromUri] factory used by the Uri based import path.
 */
// sdk 33: keeps this test in the same Robolectric sandbox as GiniCaptureUriImportTest. In the
// default sdk sandbox ImageDiskStoreTest installs a custom MimeTypeMap shadow without a resetter,
// so a MimeTypeMap singleton created with the standard shadow here would leak into that test
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageDocumentTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping("pdf", "application/pdf")
            addExtensionMimeTypeMapping("jpg", "image/jpeg")
        }
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun `fromUri creates an external source ImageDocument for an image Uri`() {
        // Given
        GiniCapture.newInstance(context).build()
        val uri = createUri(Helpers.getTestJpeg(), ".jpg")

        // When
        val document = ImageDocument.fromUri(
            uri, context, "portrait", "phone", Document.ImportMethod.OPEN_WITH
        )

        // Then
        assertThat(document.uri).isEqualTo(uri)
        assertThat(document.format).isEqualTo(ImageDocument.ImageFormat.JPEG)
        assertThat(document.importMethod).isEqualTo(Document.ImportMethod.OPEN_WITH)
        assertThat(document.source).isEqualTo(Document.Source.newExternalSource())
        assertThat(document.intent).isNull()
    }

    @Test(expected = IllegalStateException::class)
    fun `fromUri throws when the GiniCapture instance is not available`() {
        // Given
        GiniCaptureHelper.setGiniCaptureInstance(null)
        val uri = createUri(Helpers.getTestJpeg(), ".jpg")

        // When
        ImageDocument.fromUri(uri, context, "portrait", "phone", Document.ImportMethod.OPEN_WITH)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromUri throws for a Uri without an image mime type`() {
        // Given
        GiniCapture.newInstance(context).build()
        val uri = createUri("%PDF-1.4 test pdf content".toByteArray(), ".pdf")

        // When
        ImageDocument.fromUri(uri, context, "portrait", "phone", Document.ImportMethod.OPEN_WITH)
    }

    private fun createUri(bytes: ByteArray, extension: String): Uri {
        val file = File.createTempFile("gini-image-document-test", extension, context.cacheDir)
        file.writeBytes(bytes)
        tempFiles.add(file)
        return Uri.fromFile(file)
    }
}
