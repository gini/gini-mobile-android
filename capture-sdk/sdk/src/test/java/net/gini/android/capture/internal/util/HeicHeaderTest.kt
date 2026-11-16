package net.gini.android.capture.internal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [HeicHeader].
 *
 * Mirrors the iOS `DataHEICTests` suite added for PP-1430: the same five HEIF
 * brands are exercised, so both platforms agree on what counts as a HEIC.
 */
class HeicHeaderTest {

    private fun heicSignature(brand: String): ByteArray =
        byteArrayOf(0x00, 0x00, 0x00, 0x20) + // ISO-BMFF box size
            "ftyp".toByteArray(Charsets.US_ASCII) +
            brand.toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0x00, 0x00, 0x00, 0x00) // padding

    @Test
    fun `recognises every HEIF brand`() {
        listOf("heic", "heix", "heif", "mif1", "msf1").forEach { brand ->
            assertThat(HeicHeader.isHeic(heicSignature(brand))).isTrue()
        }
    }

    @Test
    fun `rejects an unknown brand behind a valid ftyp marker`() {
        assertThat(HeicHeader.isHeic(heicSignature("qt  "))).isFalse()
    }

    @Test
    fun `rejects JPEG magic bytes`() {
        val jpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        )
        assertThat(HeicHeader.isHeic(jpeg)).isFalse()
    }

    @Test
    fun `rejects arbitrary bytes`() {
        assertThat(HeicHeader.isHeic(ByteArray(32))).isFalse()
    }

    @Test
    fun `rejects a buffer shorter than the header`() {
        val truncated = heicSignature("heic").copyOf(11)
        assertThat(HeicHeader.isHeic(truncated)).isFalse()
    }

    @Test
    fun `accepts a buffer that is exactly the header length`() {
        val exact = heicSignature("heic").copyOf(12)
        assertThat(HeicHeader.isHeic(exact)).isTrue()
    }
}
