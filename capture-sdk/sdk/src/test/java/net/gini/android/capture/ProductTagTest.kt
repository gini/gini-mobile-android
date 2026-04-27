package net.gini.android.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProductTagTest {

    @Test
    fun `built in product tags use backend values`() {
        assertThat(ProductTag.SepaExtractions.value).isEqualTo("sepaExtractions")
        assertThat(ProductTag.CxExtractions.value).isEqualTo("cxExtractions")
        assertThat(ProductTag.AutoDetectExtractions.value).isEqualTo("autoDetectExtractions")
    }
}
