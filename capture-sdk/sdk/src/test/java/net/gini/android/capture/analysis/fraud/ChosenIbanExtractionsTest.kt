package net.gini.android.capture.analysis.fraud

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

class ChosenIbanExtractionsTest {

    @Test
    fun `printed IBAN replaces the iban value, drops the box, keeps the candidates and is dirty`() {
        val candidates = listOf(
            GiniCaptureExtraction("NL91ABNA0417164300", "iban", null),
            GiniCaptureExtraction("BE68539007547034", "iban", null),
        )
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "NL91ABNA0417164300", "iban", null, candidates),
        )

        val result = ChosenIbanExtractions.apply(extractions, "BE68539007547034")

        val iban = result.getValue("iban")
        assertThat(iban.name).isEqualTo("iban")
        assertThat(iban.value).isEqualTo("BE68539007547034")
        assertThat(iban.entity).isEqualTo("iban")
        assertThat(iban.box).isNull()
        assertThat(iban.candidates).isEqualTo(candidates)
        assertThat(iban.isDirty).isTrue()
    }

    @Test
    fun `QR IBAN chosen with different spacing and case keeps the same map`() {
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "FR1420041010050500013M02606", "iban", null, emptyList()),
        )

        val result = ChosenIbanExtractions.apply(extractions, "fr14 2004 1010 0505 0001 3m02 606")

        assertThat(result).isSameInstanceAs(extractions)
    }

    @Test
    fun `no choice keeps the same map`() {
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "AT611904300234573201", "iban", null, emptyList()),
        )

        assertThat(ChosenIbanExtractions.apply(extractions, null)).isSameInstanceAs(extractions)
    }

    @Test
    fun `blank choice keeps the same map`() {
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "ES9121000418450200051332", "iban", null, emptyList()),
        )

        assertThat(ChosenIbanExtractions.apply(extractions, "  ")).isSameInstanceAs(extractions)
    }

    @Test
    fun `missing iban extraction is added with the chosen IBAN`() {
        val result = ChosenIbanExtractions.apply(emptyMap(), "IT60X0542811101000000123456")

        val iban = result.getValue("iban")
        assertThat(iban.value).isEqualTo("IT60X0542811101000000123456")
        assertThat(iban.entity).isEqualTo("iban")
        assertThat(iban.candidates).isEmpty()
        assertThat(iban.isDirty).isTrue()
    }

    @Test
    fun `bic of the QR IBAN is removed and other extractions are the same instances`() {
        val amount = GiniCaptureSpecificExtraction("amountToPay", "47.65:EUR", "amount", null, emptyList())
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "DE89370400440532013000", "iban", null, emptyList()),
            "bic" to GiniCaptureSpecificExtraction("bic", "COBADEFFXXX", "bic", null, emptyList()),
            "amountToPay" to amount,
        )

        val result = ChosenIbanExtractions.apply(extractions, "DE02120300000000202051")

        assertThat(result.keys).containsExactly("iban", "amountToPay")
        assertThat(result["amountToPay"]).isSameInstanceAs(amount)
    }

    @Test
    fun `bic stays when the QR IBAN is kept`() {
        val extractions = mapOf(
            "iban" to GiniCaptureSpecificExtraction("iban", "LT601010012345678901", "iban", null, emptyList()),
            "bic" to GiniCaptureSpecificExtraction("bic", "HABALT22", "bic", null, emptyList()),
        )

        val result = ChosenIbanExtractions.apply(extractions, "LT60 1010 0123 4567 8901")

        assertThat(result.getValue("bic").value).isEqualTo("HABALT22")
    }
}
