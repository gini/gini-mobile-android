package net.gini.android.health.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.health.api.test.Helpers.loadAsset
import net.gini.android.health.api.util.ImageCompression
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageCompressionTest {

    @Test
    fun appliesCompressionIfLargerThan10MbAndIsAnImage() {
        // Given
        val imageBytes = loadAsset("invoice-12MB.png")
        val tenMBs = 10_485_760

        // When
        val compressedBytes = ImageCompression.compressIfImageAndExceedsSizeLimit(imageBytes)

        // Then
        assertThat(compressedBytes.size).isLessThan(tenMBs)
    }

    @Test
    fun doesNotApplyCompressionIfLargerThan10MbAndIsNotAnImage() {
        // Given
        val pdfBytes = loadAsset("invoice-13MB.pdf")

        // When
        val compressedBytes = ImageCompression.compressIfImageAndExceedsSizeLimit(pdfBytes)

        // Then
        assertThat(compressedBytes.size).isEqualTo(pdfBytes.size)
    }

    @Test
    fun doesNotApplyCompressionIfSmallerThan10MbAndIsAnImage() {
        // Given
        val imageBytes = loadAsset("test.jpg")

        // When
        val compressedBytes = ImageCompression.compressIfImageAndExceedsSizeLimit(imageBytes)

        // Then
        assertThat(compressedBytes.size).isEqualTo(imageBytes.size)
    }

    @Test
    fun doesNotApplyCompressionIfSmallerThan10MbAndIsNotAnImage() {
        // Given
        val pdfBytes = loadAsset("line-items.pdf")

        // When
        val compressedBytes = ImageCompression.compressIfImageAndExceedsSizeLimit(pdfBytes)

        // Then
        assertThat(compressedBytes.size).isEqualTo(pdfBytes.size)
    }
}