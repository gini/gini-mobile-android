package net.gini.android.capture.internal.util

import android.content.Context
import android.net.Uri
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Internal use only.
 *
 * Recognises HEIC/HEIF files by their container header instead of their mime
 * type.
 *
 * A content resolver usually reports the right mime type, but not always: for a
 * `file://` Uri [android.webkit.MimeTypeMap] has to guess from the extension
 * and can return `null`, and some providers report
 * `application/octet-stream`. The iOS SDK hit the same blind spot in PP-1430,
 * where a HEIC shared from the Files app was rejected because its type could
 * not be determined from the bytes either.
 *
 * A HEIC file is an ISO base media container: four bytes of box size, the ASCII
 * marker `ftyp`, then a four character brand. The five brands below are the
 * ones HEIF defines, matching the list the iOS SDK checks.
 *
 * @suppress
 */
object HeicHeader {

    private val LOG = LoggerFactory.getLogger(HeicHeader::class.java)

    /** Length of the box size field the container opens with. */
    private const val BOX_SIZE_LENGTH = 4

    /** Length of the `ftyp` marker and of a brand, four ASCII characters each. */
    private const val TAG_LENGTH = 4

    private const val FTYP_OFFSET = BOX_SIZE_LENGTH
    private const val BRAND_OFFSET = BOX_SIZE_LENGTH + TAG_LENGTH

    /**
     * Number of bytes needed to decide: box size, then the marker, then the
     * brand.
     */
    const val HEADER_LENGTH = BRAND_OFFSET + TAG_LENGTH

    private val FTYP_MARKER = "ftyp".toByteArray(Charsets.US_ASCII)

    private val HEIF_BRANDS = listOf("heic", "heix", "heif", "mif1", "msf1")
        .map { it.toByteArray(Charsets.US_ASCII) }

    /**
     * Whether [bytes] starts with a HEIF container header.
     *
     * @param bytes the beginning of a file; only the first [HEADER_LENGTH]
     * bytes are read
     */
    @JvmStatic
    fun isHeic(bytes: ByteArray): Boolean =
        bytes.size >= HEADER_LENGTH &&
            bytes.matchesAt(FTYP_OFFSET, FTYP_MARKER) &&
            HEIF_BRANDS.any { bytes.matchesAt(BRAND_OFFSET, it) }

    /**
     * Whether the file behind [uri] starts with a HEIF container header.
     *
     * Reads at most [HEADER_LENGTH] bytes. Returns `false` when the Uri cannot
     * be opened — an unreadable Uri is rejected further up the import pipeline
     * anyway.
     */
    @JvmStatic
    fun isHeic(uri: Uri, context: Context): Boolean = try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val header = ByteArray(HEADER_LENGTH)
            val read = inputStream.readAtMost(header)
            read == HEADER_LENGTH && isHeic(header)
        } ?: false
    } catch (e: IOException) {
        LOG.debug("Could not read header of uri {}", LogSanitizer.sanitize(uri), e)
        false
    } catch (e: SecurityException) {
        LOG.debug("Not permitted to read uri {}", LogSanitizer.sanitize(uri), e)
        false
    }

    private fun ByteArray.matchesAt(offset: Int, expected: ByteArray): Boolean =
        expected.indices.all { this[offset + it] == expected[it] }

    /**
     * Fills [buffer] as far as the stream allows, because a single `read` call
     * may return fewer bytes than asked for.
     */
    private fun java.io.InputStream.readAtMost(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read == -1) {
                break
            }
            total += read
        }
        return total
    }
}
