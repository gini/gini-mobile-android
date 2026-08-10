package net.gini.android.core.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocumentMetadataUnitTest {

    @Test
    fun `setProductTag uses backend header name`() {
        val documentMetadata = DocumentMetadata()

        documentMetadata.setProductTag("sepaExtractions")

        assertThat(documentMetadata.metadata).containsEntry(
            DocumentMetadata.PRODUCT_TAG_HEADER_FIELD_NAME,
            "sepaExtractions"
        )
    }

    @Test
    fun `copy preserves product tag header name`() {
        val documentMetadata = DocumentMetadata().apply {
            setProductTag("cxExtractions")
        }

        val copy = documentMetadata.copy()

        assertThat(copy.metadata).containsEntry(
            DocumentMetadata.PRODUCT_TAG_HEADER_FIELD_NAME,
            "cxExtractions"
        )
    }
}
