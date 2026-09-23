package net.gini.android.capture.internal.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for the HEIC support [FileImportValidator] gained in PP-3499.
 *
 * The PDF criteria are covered by the instrumented `FileImportValidatorTest`,
 * which needs a real PDF renderer. What is tested here is the accept/reject
 * decision per mime type and per Android version, which Robolectric can drive
 * through `@Config(sdk = ...)` without two devices.
 */
@RunWith(RobolectricTestRunner::class)
class FileImportValidatorTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping("heic", "image/heic")
            addExtensionMimeTypeMapping("jpg", "image/jpeg")
            addExtensionMimeTypeMapping("webp", "image/webp")
        }
    }

    @After
    fun tearDown() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    @Config(sdk = [28])
    fun `accepts a heic file on api 28`() {
        val uri = createUri(heicBytes(), ".heic")

        assertThat(validator().matchesCriteria(uri)).isTrue()
    }

    @Test
    @Config(sdk = [27])
    fun `rejects a heic file below api 28 with type not supported`() {
        val uri = createUri(heicBytes(), ".heic")
        val validator = validator()

        assertThat(validator.matchesCriteria(uri)).isFalse()
        assertThat(validator.error).isEqualTo(FileImportValidator.Error.TYPE_NOT_SUPPORTED)
    }

    @Test
    @Config(sdk = [28])
    fun `accepts a heic file whose mime type cannot be resolved`() {
        // A file manager sharing a file:// Uri with an unhelpful extension:
        // only the header identifies it as a HEIC.
        val uri = createUri(heicBytes(), ".bin")

        assertThat(validator().matchesCriteria(uri)).isTrue()
    }

    @Test
    @Config(sdk = [28])
    fun `still rejects an image type that stays unsupported`() {
        val uri = createUri("RIFF____WEBPVP8 ".toByteArray(), ".webp")
        val validator = validator()

        assertThat(validator.matchesCriteria(uri)).isFalse()
        assertThat(validator.error).isEqualTo(FileImportValidator.Error.TYPE_NOT_SUPPORTED)
    }

    @Test
    @Config(sdk = [28])
    fun `still accepts jpeg`() {
        val uri = createUri(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), ".jpg")

        assertThat(validator().matchesCriteria(uri)).isTrue()
    }

    @Test
    @Config(sdk = [28])
    fun `rejects a heic file over the size limit`() {
        val uri = createUri(heicBytes() + ByteArray(64), ".heic")
        val validator = FileImportValidator(context, 16)

        assertThat(validator.matchesCriteria(uri)).isFalse()
        assertThat(validator.error).isEqualTo(FileImportValidator.Error.SIZE_TOO_LARGE)
    }

    private fun validator() = FileImportValidator(context, FILE_SIZE_LIMIT)

    /** Minimal ISO-BMFF header: box size, the `ftyp` marker and the `heic` brand. */
    private fun heicBytes(): ByteArray =
        byteArrayOf(0x00, 0x00, 0x00, 0x20) +
            "ftyp".toByteArray(Charsets.US_ASCII) +
            "heic".toByteArray(Charsets.US_ASCII) +
            ByteArray(4)

    private fun createUri(bytes: ByteArray, extension: String): Uri {
        val file = File.createTempFile("gini-file-import-validator", extension, context.cacheDir)
        file.writeBytes(bytes)
        tempFiles.add(file)
        return Uri.fromFile(file)
    }
}
