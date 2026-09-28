package net.gini.android.capture.network

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

/**
 * Unit tests for [AnalysisResult]: the four-argument constructor carries the extractions and the
 * deprecated five-argument constructor ignores the return reasons it is given.
 */
@Suppress("DEPRECATION")
class AnalysisResultTest {

    private val specificExtractions =
        mapOf("amountToPay" to mockk<GiniCaptureSpecificExtraction>())
    private val compoundExtractions =
        mapOf("lineItems" to mockk<GiniCaptureCompoundExtraction>())

    @Test
    fun `four-argument constructor carries the document and its extractions`() {
        val result = AnalysisResult("doc-id", "invoice.pdf", specificExtractions, compoundExtractions)

        assertThat(result.giniApiDocumentId).isEqualTo("doc-id")
        assertThat(result.giniApiDocumentFilename).isEqualTo("invoice.pdf")
        assertThat(result.extractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }

    @Test
    fun `deprecated five-argument constructor ignores the return reasons`() {
        val returnReasons = listOf(GiniCaptureReturnReason("r1", mapOf("de" to "Beschädigt")))

        val result = AnalysisResult(
            "doc-id",
            "invoice.pdf",
            specificExtractions,
            compoundExtractions,
            returnReasons
        )

        assertThat(result.extractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }
}
