package net.gini.android.capture.internal.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * The Uri based half of [HeicHeader]: what happens between the content resolver and the byte
 * check. The byte check itself is pinned by [HeicHeaderTest].
 *
 * An unreadable Uri must answer `false` rather than throw — the import pipeline rejects it with a
 * proper validation error further up, and a crash here would pre-empt that.
 */
@RunWith(RobolectricTestRunner::class)
class HeicHeaderUriTest {

    private lateinit var context: Context
    private lateinit var resolver: ShadowContentResolver
    private val uri: Uri = Uri.parse("content://net.gini.android.capture.test/heic-header")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resolver = shadowOf(context.contentResolver)
    }

    @Test
    fun `recognises a HEIC behind a content Uri`() {
        resolver.registerInputStream(uri, ByteArrayInputStream(heicSignature("heic") + ByteArray(20)))

        assertThat(HeicHeader.isHeic(uri, context)).isTrue()
    }

    @Test
    fun `rejects a JPEG behind a content Uri`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(28)
        resolver.registerInputStream(uri, ByteArrayInputStream(jpeg))

        assertThat(HeicHeader.isHeic(uri, context)).isFalse()
    }

    /** A single `read` may return fewer bytes than asked for; the header must still be assembled. */
    @Test
    fun `assembles the header from a stream that delivers one byte at a time`() {
        resolver.registerInputStream(uri, OneByteAtATimeInputStream(heicSignature("heic")))

        assertThat(HeicHeader.isHeic(uri, context)).isTrue()
    }

    @Test
    fun `rejects a stream that ends before the header is complete`() {
        resolver.registerInputStream(uri, ByteArrayInputStream(heicSignature("heic").copyOf(4)))

        assertThat(HeicHeader.isHeic(uri, context)).isFalse()
    }

    @Test
    fun `rejects a Uri that yields no stream`() {
        // Robolectric's shadow resolver never answers null, so this needs a stand-in
        val nullResolver = mockk<ContentResolver> { every { openInputStream(uri) } returns null }
        val nullContext = mockk<Context> { every { contentResolver } returns nullResolver }

        assertThat(HeicHeader.isHeic(uri, nullContext)).isFalse()
    }

    @Test
    fun `rejects a Uri whose stream cannot be read`() {
        resolver.registerInputStream(uri, object : InputStream() {
            override fun read(): Int = throw IOException("disk gone")
        })

        assertThat(HeicHeader.isHeic(uri, context)).isFalse()
    }

    @Test
    fun `rejects a Uri the app is not permitted to read`() {
        resolver.registerInputStreamSupplier(uri) { throw SecurityException("no permission") }

        assertThat(HeicHeader.isHeic(uri, context)).isFalse()
    }

    private fun heicSignature(brand: String): ByteArray =
        byteArrayOf(0x00, 0x00, 0x00, 0x20) +
            "ftyp".toByteArray(Charsets.US_ASCII) +
            brand.toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0x00, 0x00, 0x00, 0x00)

    private class OneByteAtATimeInputStream(private val bytes: ByteArray) : InputStream() {
        private var position = 0

        override fun read(): Int = if (position < bytes.size) bytes[position++].toInt() and 0xFF else -1

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            val next = read()
            if (next == -1) return -1
            b[off] = next.toByte()
            return 1
        }
    }
}
