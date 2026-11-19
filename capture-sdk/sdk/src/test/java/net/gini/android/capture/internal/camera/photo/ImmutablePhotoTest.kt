package net.gini.android.capture.internal.camera.photo

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.Document
import net.gini.android.capture.document.ImageDocument
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImmutablePhotoTest {

    private val cacheDir: File
        get() = ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir

    @Test
    fun `saves its image data to a file`() {
        val jpeg = newJpeg()
        val file = File(cacheDir, "photo.jpg")

        newPhoto(jpeg).saveToFile(file)

        assertThat(file.readBytes()).isEqualTo(jpeg)
    }

    @Test
    fun `does not write a file when the image data cannot be written`() {
        val missingDirectory = File(cacheDir, "missing")

        newPhoto(newJpeg()).saveToFile(File(missingDirectory, "photo.jpg"))

        assertThat(missingDirectory.exists()).isFalse()
    }

    @Test
    fun `does not write a file when it has no image data`() {
        val file = File(cacheDir, "no-data.jpg")
        val documentWithoutData = mockk<ImageDocument>(relaxed = true) {
            every { data } returns null
            every { format } returns ImageDocument.ImageFormat.JPEG
        }
        val photo = ImmutablePhoto(documentWithoutData)

        photo.saveToFile(file)

        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `saves its preview to a file`() {
        val file = File(cacheDir, "preview.jpg")

        newPhoto(newJpeg()).savePreviewToFile(file)

        assertThat(file.length()).isGreaterThan(0L)
    }

    @Test
    fun `does not write a preview file into a missing directory`() {
        val missingDirectory = File(cacheDir, "missing-preview")

        newPhoto(newJpeg()).savePreviewToFile(File(missingDirectory, "preview.jpg"))

        assertThat(missingDirectory.exists()).isFalse()
    }

    private fun newPhoto(jpeg: ByteArray) =
        ImmutablePhoto(jpeg, 0, ImageDocument.ImageFormat.JPEG, false, Document.ImportMethod.NONE)

    private fun newJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }

    private companion object {
        const val BITMAP_SIZE = 16
        const val JPEG_QUALITY = 90
    }
}
