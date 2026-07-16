package net.gini.android.core.api.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.core.api.models.Document
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtractionsParserTest {

    @Test
    fun `parses specific extractions with candidates and boxes`() {
        val container = ExtractionsParser.parseExtractionsContainer(JSONObject(EXTRACTIONS_JSON))

        val amountToPay = container.specificExtractions["amountToPay"]
        assertThat(amountToPay).isNotNull()
        assertThat(amountToPay!!.value).isEqualTo("335.50:EUR")
        assertThat(amountToPay.entity).isEqualTo("amount")
        assertThat(amountToPay.box).isNotNull()
        assertThat(amountToPay.box!!.pageNumber).isEqualTo(1)
        assertThat(amountToPay.box!!.left).isEqualTo(516.0)
        assertThat(amountToPay.candidate).hasSize(2)
        assertThat(amountToPay.candidate[1].value).isEqualTo("23.00:EUR")
    }

    @Test
    fun `parses compound extractions`() {
        val container = ExtractionsParser.parseExtractionsContainer(JSONObject(EXTRACTIONS_JSON))

        val lineItems = container.compoundExtractions["lineItems"]
        assertThat(lineItems).isNotNull()
        assertThat(lineItems!!.specificExtractionMaps).hasSize(1)
        assertThat(lineItems.specificExtractionMaps[0]["description"]?.value).isEqualTo("Shoes")
    }

    @Test
    fun `parses response without compound extractions to an empty map`() {
        val json = JSONObject("""{"extractions":{},"candidates":{}}""")

        val container = ExtractionsParser.parseExtractionsContainer(json)

        assertThat(container.specificExtractions).isEmpty()
        assertThat(container.compoundExtractions).isEmpty()
    }

    @Test
    fun `parses extraction without box`() {
        val extraction = ExtractionsParser.parseExtraction(
            JSONObject("""{"entity":"text","value":"Shoes"}""")
        )

        assertThat(extraction.value).isEqualTo("Shoes")
        assertThat(extraction.entity).isEqualTo("text")
        assertThat(extraction.box).isNull()
    }

    companion object {
        val EXTRACTIONS_JSON = """
            {
                "extractions": {
                    "amountToPay": {
                        "box": { "height": 9.0, "left": 516.0, "page": 1, "top": 588.0, "width": 42.0 },
                        "entity": "amount",
                        "value": "335.50:EUR",
                        "candidates": "amounts"
                    }
                },
                "candidates": {
                    "amounts": [
                        {
                            "box": { "height": 9.0, "left": 516.0, "page": 1, "top": 588.0, "width": 42.0 },
                            "entity": "amount",
                            "value": "335.50:EUR"
                        },
                        {
                            "box": { "height": 9.0, "left": 241.0, "page": 1, "top": 588.0, "width": 42.0 },
                            "entity": "amount",
                            "value": "23.00:EUR"
                        }
                    ]
                },
                "compoundExtractions": {
                    "lineItems": [
                        {
                            "description": {
                                "entity": "text",
                                "value": "Shoes"
                            }
                        }
                    ]
                }
            }
        """.trimIndent()
    }
}

@RunWith(AndroidJUnit4::class)
class CompositeDocumentJsonTest {

    @Test
    fun `creates composite json with document uris and normalized rotations`() {
        val documentRotationMap = linkedMapOf(
            document("partial-1") to 450,
            document("partial-2") to -90
        )

        val json = JSONObject(String(CompositeDocumentJson.create(documentRotationMap), Charsets.UTF_8))

        val partialDocuments = json.getJSONArray("partialDocuments")
        assertThat(partialDocuments.length()).isEqualTo(2)
        assertThat(partialDocuments.getJSONObject(0).getString("document"))
            .isEqualTo("https://pay-api.gini.net/documents/partial-1")
        assertThat(partialDocuments.getJSONObject(0).getInt("rotationDelta")).isEqualTo(90)
        assertThat(partialDocuments.getJSONObject(1).getString("document"))
            .isEqualTo("https://pay-api.gini.net/documents/partial-2")
        assertThat(partialDocuments.getJSONObject(1).getInt("rotationDelta")).isEqualTo(270)
    }

    @Test
    fun `creates composite json with zero rotation for a document list`() {
        val json = JSONObject(
            String(CompositeDocumentJson.create(listOf(document("partial-1"))), Charsets.UTF_8)
        )

        val partialDocuments = json.getJSONArray("partialDocuments")
        assertThat(partialDocuments.length()).isEqualTo(1)
        assertThat(partialDocuments.getJSONObject(0).getInt("rotationDelta")).isEqualTo(0)
    }

    private fun document(id: String) = Document.fromApiResponse(
        JSONObject(
            """
            {
                "id": "$id",
                "progress": "COMPLETED",
                "pageCount": 1,
                "name": "invoice.jpg",
                "creationDate": 1515932941283,
                "sourceClassification": "NATIVE",
                "_links": {
                    "document": "https://pay-api.gini.net/documents/$id",
                    "extractions": "https://pay-api.gini.net/documents/$id/extractions"
                }
            }
            """.trimIndent()
        )
    )
}
