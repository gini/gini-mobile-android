package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.camera.photo.Exif.userCommentBuilder
import net.gini.android.capture.internal.camera.photo.ExifUserCommentHelper.Companion.getValueForKeyFromUserComment
import net.gini.android.capture.test.Helpers
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.Charset
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
class ExifTest {

    @Test
    @Throws(Exception::class)
    fun `reads required string tags correctly even if they contain null bytes`() {
        // Given
        // Test jpeg make and model tags contain null bytes
        val testJpeg = Helpers.loadAsset("exif-string-tag-with-null-bytes.jpeg")
        val requiredTags = Exif.readRequiredTags(testJpeg)

        // When
        val exif = Exif.builder(testJpeg).setRequiredTags(requiredTags).build()

        // Then
        val outJpeg = exif.writeToJpeg(testJpeg)
        val outRequiredTags = Exif.readRequiredTags(outJpeg)
        Truth.assertThat(outRequiredTags.make.value as Array<*>).asList().contains("Lenovo")
        Truth.assertThat(outRequiredTags.model.value as Array<*>).asList().contains(
            "Lenovo TAB 2 A10-70F"
        )
    }

    @Test
    fun `writes entry point to user comment`() {
        // Given
        val testJpeg = Helpers.loadAsset("invoice-without-exif-user-comment.jpg")
        val exif = Exif.builder(testJpeg)
            .setUserComment(
                userCommentBuilder()
                    .setContentId("AB5C10CC-8F78-4ABF-91EC-3B2E90FB918A")
                    .build()
            )
            .build()
        val jpegWithEntryPoint = exif.writeToJpeg(testJpeg)

        // When
        val entryPointValue = getValueForKeyFromJpegExifUserComment(Exif.USER_COMMENT_ENTRY_POINT, jpegWithEntryPoint)

        // Then
        Truth.assertThat(EntryPoint.valueOf(entryPointValue.uppercase())).isEqualTo(GiniCapture.Internal.DEFAULT_ENTRY_POINT)
    }

    private fun getValueForKeyFromJpegExifUserComment(key: String, jpegBytes: ByteArray): String {
        val jpegMetadata = Imaging.getMetadata(jpegBytes) as JpegImageMetadata
        val userCommentField = jpegMetadata.findEXIFValue(ExifTagConstants.EXIF_TAG_USER_COMMENT)
        val rawUserComment = userCommentField.byteArrayValue
        val userCommentString = String(
            Arrays.copyOfRange(rawUserComment, 8, rawUserComment.size),
            Charset.forName("US-ASCII")
        )
        return getValueForKeyFromUserComment(key, userCommentString)
    }
}