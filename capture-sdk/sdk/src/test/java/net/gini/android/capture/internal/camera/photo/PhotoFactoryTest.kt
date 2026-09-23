package net.gini.android.capture.internal.camera.photo

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.document.DocumentFactory
import net.gini.android.capture.document.ImageDocument
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
 * Unit tests for [PhotoFactory].
 *
 * The factory decides which images get a real [PhotoEdit] — and therefore get
 * re-encoded to JPEG — and which keep the no-op edit and travel untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhotoFactoryTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping("jpg", "image/jpeg")
            addExtensionMimeTypeMapping("heic", "image/heic")
            addExtensionMimeTypeMapping("png", "image/png")
            addExtensionMimeTypeMapping("gif", "image/gif")
        }
        GiniCapture.newInstance(context).build()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun `a heic document gets an editable photo so it can be re-encoded`() {
        val photo = PhotoFactory.newPhotoFromDocument(documentWithExtension(".heic"))

        assertThat(photo.imageFormat).isEqualTo(ImageDocument.ImageFormat.HEIC)
        assertThat(photo.edit()).isNotInstanceOf(NoOpPhotoEdit::class.java)
    }

    @Test
    fun `a jpeg document still gets an editable photo`() {
        val photo = PhotoFactory.newPhotoFromDocument(documentWithExtension(".jpg"))

        assertThat(photo.imageFormat).isEqualTo(ImageDocument.ImageFormat.JPEG)
        assertThat(photo.edit()).isNotInstanceOf(NoOpPhotoEdit::class.java)
    }

    @Test
    fun `png and gif documents keep the no-op edit and are left untouched`() {
        listOf(".png", ".gif").forEach { extension ->
            val photo = PhotoFactory.newPhotoFromDocument(documentWithExtension(extension))

            assertThat(photo.edit()).isInstanceOf(NoOpPhotoEdit::class.java)
        }
    }

    /**
     * Builds a document whose declared format comes from the file extension
     * while the bytes stay a real JPEG, so the photo can be decoded in tests
     * without a HEIF decoder.
     */
    private fun documentWithExtension(extension: String): ImageDocument {
        val file = File.createTempFile("gini-photo-factory-test", extension, context.cacheDir)
        file.writeBytes(Helpers.getTestJpeg())
        tempFiles.add(file)
        return DocumentFactory.newImageDocumentFromUri(
            Uri.fromFile(file), context, "portrait", "phone", Document.ImportMethod.PICKER
        ).apply { data = Helpers.getTestJpeg() }
    }
}
