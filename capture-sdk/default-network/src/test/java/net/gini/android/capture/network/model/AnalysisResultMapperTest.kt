package net.gini.android.capture.network.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisResultMapperTest {

    @Test
    fun `maps extractions response json to an analysis result`() {
        val analysisResult = AnalysisResultMapper.fromExtractionsJson(
            documentId = "document-id-13",
            documentFilename = "invoice.jpg",
            extractionsResponseJson = EXTRACTIONS_JSON
        )

        assertThat(analysisResult.giniApiDocumentId).isEqualTo("document-id-13")
        assertThat(analysisResult.giniApiDocumentFilename).isEqualTo("invoice.jpg")

        val amountToPay = analysisResult.extractions["amountToPay"]
        assertThat(amountToPay).isNotNull()
        assertThat(amountToPay!!.value).isEqualTo("335.50:EUR")
        assertThat(amountToPay.entity).isEqualTo("amount")
        assertThat(amountToPay.box).isNotNull()

        val lineItems = analysisResult.compoundExtractions["lineItems"]
        assertThat(lineItems).isNotNull()
        assertThat(lineItems!!.specificExtractionMaps).hasSize(1)
        assertThat(lineItems.specificExtractionMaps[0]["description"]?.value).isEqualTo("Shoes")

        assertThat(analysisResult.returnReasons).hasSize(1)
        assertThat(analysisResult.returnReasons[0].id).isEqualTo("r1")
        assertThat(analysisResult.returnReasons[0].localizedLabels["de"]).isEqualTo("Beschädigt")
    }

    companion object {
        val EXTRACTIONS_JSON = """
            {
                "extractions": {
                    "amountToPay": {
                        "box": { "height": 9.0, "left": 516.0, "page": 1, "top": 588.0, "width": 42.0 },
                        "entity": "amount",
                        "value": "335.50:EUR"
                    }
                },
                "candidates": {},
                "compoundExtractions": {
                    "lineItems": [
                        {
                            "description": {
                                "entity": "text",
                                "value": "Shoes"
                            }
                        }
                    ]
                },
                "returnReasons": [
                    {
                        "id": "r1",
                        "de": "Beschädigt"
                    }
                ]
            }
        """.trimIndent()
    }
}
