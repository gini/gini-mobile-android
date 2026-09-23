package net.gini.android.capture.internal.fileimport

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ImportImageFileUrisAsyncTask
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.test.Helpers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of the HEIC import added in PP-3499.
 *
 * These run on a device because Robolectric ships no HEIF decoder: everything
 * that matters here — that a real HEIC is decoded and comes back out as JPEG,
 * upright, with its pixels intact — needs the platform decoder that arrived in
 * Android 9.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class ImportHeicUriTest {

    private companion object {
        const val HEIC = "invoice.heic"
        const val HEIC_ROTATED = "invoice-rotated.heic"
        const val HEIC_BURST = "invoice-burst.heic"
        const val HEIC_TRUNCATED = "invoice-truncated.heic"
        const val TIMEOUT_SECONDS = 30L
    }

    private lateinit var context: Context
    private val copiedAssets = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GiniCapture.newInstance(context).setMultiPageEnabled(true).build()
    }

    @After
    fun tearDown() {
        copiedAssets.forEach { Helpers.deleteAssetFileFromContentUri(it) }
        copiedAssets.clear()
        GiniCapture.cleanup(context)
    }

    @Test
    fun heicIsAcceptedAndConvertedToJpeg() {
        val document = importSingle(HEIC)

        assertThat(document.mimeType).isEqualTo("image/jpeg")
        assertThat(document.format).isEqualTo(ImageDocument.ImageFormat.JPEG)
        // JPEG start-of-image marker: these are the bytes the network layer
        // hands to the Gini API as the partial document's content.
        val data = requireNotNull(document.data)
        assertThat(data[0]).isEqualTo(0xFF.toByte())
        assertThat(data[1]).isEqualTo(0xD8.toByte())
    }

    @Test
    fun convertedJpegKeepsThePixelDimensionsOfTheHeic() {
        val source = decodeBounds(Helpers.loadAsset(HEIC))

        val document = importSingle(HEIC)

        val result = decodeBounds(requireNotNull(document.data))
        assertThat(result.outWidth).isEqualTo(source.outWidth)
        assertThat(result.outHeight).isEqualTo(source.outHeight)
        // Guards against the assertion above passing on a decode failure.
        assertThat(result.outWidth).isGreaterThan(0)
    }

    @Test
    fun rotatedHeicKeepsItsOrientation() {
        val document = importSingle(HEIC_ROTATED)

        // The fixture carries EXIF orientation 6, which is 90 degrees clockwise.
        assertThat(document.rotationForDisplay).isEqualTo(90)
    }

    @Test
    fun multiImageHeicContainerYieldsExactlyOneDocument() {
        val result = import(HEIC_BURST)

        assertThat(result.document?.documents).hasSize(1)
        assertThat(result.document!!.documents[0].mimeType).isEqualTo("image/jpeg")
    }

    @Test
    fun truncatedHeicIsReportedAsAnErrorInsteadOfBeingUploaded() {
        val result = import(HEIC_TRUNCATED)

        // A HEIC that cannot be decoded cannot be converted, and the Gini API
        // does not accept HEIC - so it has to be refused here rather than
        // uploaded as it is.
        assertThat(result.exception).isNotNull()
        assertThat(result.exception!!.validationError)
            .isEqualTo(FileImportValidator.Error.TYPE_NOT_SUPPORTED)
        assertThat(result.document?.documents.orEmpty()).isEmpty()
    }

    private fun importSingle(assetName: String): ImageDocument {
        val result = import(assetName)
        assertThat(result.exception).isNull()
        return requireNotNull(result.document).documents.single()
    }

    private fun import(assetName: String): ImportResult {
        val uri = contentUri(assetName)
        var document: ImageMultiPageDocument? = null
        var exception: ImportedFileValidationException? = null
        val latch = CountDownLatch(1)

        val task = ImportImageFileUrisAsyncTask(
            context,
            GiniCapture.getInstance(),
            Document.Source.newExternalSource(),
            Document.ImportMethod.OPEN_WITH,
            object : AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException> {
                override fun onSuccess(result: ImageMultiPageDocument) {
                    document = result
                    latch.countDown()
                }

                override fun onError(e: ImportedFileValidationException) {
                    exception = e
                    latch.countDown()
                }

                override fun onCancelled() {
                    latch.countDown()
                }
            }
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            @Suppress("DEPRECATION")
            task.execute(uri)
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        return ImportResult(document, exception)
    }

    private data class ImportResult(
        val document: ImageMultiPageDocument?,
        val exception: ImportedFileValidationException?
    )

    private fun contentUri(assetName: String): Uri {
        copiedAssets.add(assetName)
        return Helpers.getAssetFileFileContentUri(assetName)
    }

    private fun decodeBounds(bytes: ByteArray): BitmapFactory.Options =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
        }
}
