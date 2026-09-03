package net.gini.android.capture.analysis.warning

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BusinessDocTypeTest {

    @Test
    fun `from returns CREDIT_NOTE for CreditNote`() {
        assertThat(BusinessDocType.from("CreditNote")).isEqualTo(BusinessDocType.CREDIT_NOTE)
    }

    @Test
    fun `from trims surrounding whitespace before matching`() {
        assertThat(BusinessDocType.from("  CreditNote  ")).isEqualTo(BusinessDocType.CREDIT_NOTE)
    }

    @Test
    fun `from returns UNKNOWN for null`() {
        assertThat(BusinessDocType.from(null)).isEqualTo(BusinessDocType.UNKNOWN)
    }

    @Test
    fun `from returns UNKNOWN for empty string`() {
        assertThat(BusinessDocType.from("")).isEqualTo(BusinessDocType.UNKNOWN)
    }

    @Test
    fun `from returns UNKNOWN for other document types`() {
        assertThat(BusinessDocType.from("Invoice")).isEqualTo(BusinessDocType.UNKNOWN)
    }

    @Test
    fun `isCreditNote is true only for CREDIT_NOTE`() {
        assertThat(BusinessDocType.CREDIT_NOTE.isCreditNote()).isTrue()
        assertThat(BusinessDocType.UNKNOWN.isCreditNote()).isFalse()
    }
}
