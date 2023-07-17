package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import net.gini.android.capture.test.Helpers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExifReaderTest {

    @Test
    @Throws(Exception::class)
    fun `reads user comment`() {
        val testJpeg = Helpers.loadAsset("remslip-valid-user-comment.jpeg")
        val exifReader = ExifReader.forJpeg(testJpeg)
        Truth.assertThat(exifReader.userComment).isEqualTo("This is valid")
    }

    @Test
    @Throws(Exception::class)
    fun `reads user comment if length is too short`() {
        // Minimum length is 8 bytes (character code length), following image has 5 bytes
        val testJpeg = Helpers.loadAsset("remslip-malformed-user-comment.jpeg")
        val exifReader = ExifReader.forJpeg(testJpeg)
        Truth.assertThat(exifReader.userComment).isEqualTo("short")
    }

    @Test
    @Throws(Exception::class)
    fun `throws exception if metadata was missing`() {
        val testJpeg = Helpers.loadAsset("remslip-no-metadata.jpeg")
        var exception: ExifReaderException? = null
        try {
            ExifReader.forJpeg(testJpeg)
        } catch (e: ExifReaderException) {
            exception = e
        }
        Truth.assertThat(exception).isNotNull()
        Truth.assertThat(exception!!.message).isEqualTo("No jpeg metadata found")
    }

    @Test
    @Throws(Exception::class)
    fun `throws exception if user comment was missing`() {
        // Given
        val testJpeg = Helpers.loadAsset("remslip-no-user-comment.jpeg")
        val exifReader = ExifReader.forJpeg(testJpeg)
        var exception: ExifReaderException? = null
        try {
            exifReader.userComment
        } catch (e: ExifReaderException) {
            exception = e
        }
        Truth.assertThat(exception).isNotNull()
        Truth.assertThat(exception!!.message).isEqualTo("No User Comment found")
    }

    @Test
    @Throws(Exception::class)
    fun `throws exception if byte array was not jpeg`() {
        // Given
        var exception: ExifReaderException? = null
        try {
            val notJpeg = byteArrayOf(0, 2, 3, 1)
            ExifReader.forJpeg(notJpeg)
        } catch (e: ExifReaderException) {
            exception = e
        }
        Truth.assertThat(exception).isNotNull()
        Truth.assertThat(exception!!.message).startsWith("Could not read jpeg metadata: ")
    }

    @Test
    @Throws(Exception::class)
    fun `returns value for key in user comment CSV`() {
        // Given
        val userComment =
            "Platform=Android,OSVer=7.0,GiniVisionVer=2.1.0(SNAPSHOT),ContentId=21e5bc66-ee46-4ec4-93db-16bd553561bf,RotDeltaDeg=0"
        // When
        val value = ExifReader.getValueForKeyFromUserComment(
            "OSVer", userComment
        )
        // Then
        Truth.assertThat(value).isEqualTo("7.0")
    }

    @Test
    @Throws(Exception::class)
    fun `returns null if key was not found in user comment CSV`() {
        // Given
        val userComment = "no such key here"
        // When
        val value = ExifReader.getValueForKeyFromUserComment(
            "unknownKey", userComment
        )
        // Then
        Truth.assertThat(value).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun `returns null if user comment CSV is empty`() {
        // Given
        val userComment = ""
        // When
        val value = ExifReader.getValueForKeyFromUserComment("", userComment)
        // Then
        Truth.assertThat(value).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun `returns null if user comment CSV is malformed`() {
        // Given
        val userComment = ",Key1=OSVer=,"
        // When
        val value = ExifReader.getValueForKeyFromUserComment("OSVer", userComment)
        // Then
        Truth.assertThat(value).isNull()
    }
}