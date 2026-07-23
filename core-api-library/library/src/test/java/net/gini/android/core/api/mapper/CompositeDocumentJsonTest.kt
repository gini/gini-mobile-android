package net.gini.android.core.api.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.core.api.models.Document
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

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
