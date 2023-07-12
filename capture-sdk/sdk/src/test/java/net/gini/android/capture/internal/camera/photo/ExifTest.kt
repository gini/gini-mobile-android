package net.gini.android.capture.internal.camera.photo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import net.gini.android.capture.test.Helpers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExifTest {

    @Test
    @Throws(Exception::class)
    fun `read required string tags correctly even if they contain null bytes`() {
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
}