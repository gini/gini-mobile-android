package net.gini.android.capture.analysis.fraud

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.fraudSignals.FraudSignal
import net.gini.android.capture.fraudSignals.FraudSignalCode
import net.gini.android.capture.fraudSignals.FraudSignalSeverity
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

class IBANMismatchArgsMapperTest {

    @Test
    fun `QR code signal shows the QR and printed IBANs without hints or sender`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(qrSignal("PL61109010140000071219812874")),
            ibanCandidates("PL61109010140000071219812874", "DE89370400440532013000"),
        )

        assertThat(args).isEqualTo(
            IBANMismatchArgs(
                type = IBANMismatchType.QR_VS_PRINTED,
                qrIban = IBANMismatchIban("PL61109010140000071219812874"),
                printedIbans = listOf(IBANMismatchIban("DE89370400440532013000")),
            )
        )
    }

    @Test
    fun `country signal shows the document IBAN with hint and the sender`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(countrySignal("LT601010012345678901", country = "AT")),
            recipient("Holzbau Steiner GmbH"),
        )

        assertThat(args).isEqualTo(
            IBANMismatchArgs(
                type = IBANMismatchType.COUNTRY,
                qrIban = null,
                printedIbans = listOf(IBANMismatchIban("LT601010012345678901", hintCountryCode = "LT")),
                senderName = "Holzbau Steiner GmbH",
                senderCountryCode = "AT",
            )
        )
    }

    @Test
    fun `both signals show one combined screen with hints only on foreign IBANs`() {
        val qrIban = "PL12100001110100100012345678"
        val args = IBANMismatchArgsMapper.map(
            listOf(qrSignal(qrIban), countrySignal(qrIban, country = "DE")),
            ibanCandidates("DE02120300000000202051", "LT601010012345678901") +
                recipient("Elektro Brandt GmbH"),
        )

        assertThat(args).isEqualTo(
            IBANMismatchArgs(
                type = IBANMismatchType.COMBINED,
                qrIban = IBANMismatchIban(qrIban, hintCountryCode = "PL"),
                printedIbans = listOf(
                    IBANMismatchIban("DE02120300000000202051"),
                    IBANMismatchIban("LT601010012345678901", hintCountryCode = "LT"),
                ),
                senderName = "Elektro Brandt GmbH",
                senderCountryCode = "DE",
            )
        )
    }

    @Test
    fun `printed IBANs keep document order without the QR IBAN and duplicates`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(qrSignal("NL91ABNA0417164300")),
            ibanCandidates(
                "FR1420041010050500013M02606",
                "nl91 abna 0417 1643 00",
                "BE68539007547034",
                "FR14 2004 1010 0505 0001 3M02 606",
            ),
        )

        assertThat(args?.printedIbans?.map { it.iban })
            .containsExactly("FR1420041010050500013M02606", "BE68539007547034")
            .inOrder()
    }

    @Test
    fun `QR code signal without an iban extraction shows no warning`() {
        val args = IBANMismatchArgsMapper.map(listOf(qrSignal("ES9121000418450200051332")), emptyMap())

        assertThat(args).isNull()
    }

    @Test
    fun `QR code signal whose only candidate is the QR IBAN falls back to the country screen`() {
        val iban = "IT60X0542811101000000123456"
        val args = IBANMismatchArgsMapper.map(
            listOf(qrSignal(iban), countrySignal(iban, country = "CH")),
            ibanCandidates(iban),
        )

        assertThat(args?.type).isEqualTo(IBANMismatchType.COUNTRY)
        assertThat(args?.qrIban).isNull()
        assertThat(args?.printedIbans).containsExactly(IBANMismatchIban(iban, hintCountryCode = "IT"))
    }

    @Test
    fun `country signal without payment recipient keeps the sender country only`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(countrySignal("SE4550000000058398257466", country = "DK")),
            mapOf("paymentRecipient" to text("paymentRecipient", "  ")),
        )

        assertThat(args?.senderName).isNull()
        assertThat(args?.senderCountryCode).isEqualTo("DK")
    }

    @Test
    fun `no signals show no warning`() {
        val args = IBANMismatchArgsMapper.map(emptyList(), ibanCandidates("DE89370400440532013000"))

        assertThat(args).isNull()
    }

    @Test
    fun `country codes are compared case-insensitively`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(countrySignal("de89370400440532013000", country = "de")),
            emptyMap(),
        )

        assertThat(args?.senderCountryCode).isEqualTo("DE")
        assertThat(args?.printedIbans?.single()?.hintCountryCode).isNull()
    }

    @Test
    fun `blank and too short IBANs do not crash and get no hint`() {
        val args = IBANMismatchArgsMapper.map(
            listOf(qrSignal("PT50000201231234567890154"), countrySignal("PT50000201231234567890154", "FR")),
            ibanCandidates(" ", "9"),
        )

        assertThat(args?.type).isEqualTo(IBANMismatchType.COMBINED)
        assertThat(args?.printedIbans).containsExactly(IBANMismatchIban("9"))
    }

    private fun qrSignal(iban: String) = FraudSignal(
        code = FraudSignalCode.QR_PRINTED_IBAN_MISMATCH,
        severity = FraudSignalSeverity.STOP,
        iban = iban,
        creditorCountry = null,
    )

    private fun countrySignal(iban: String, country: String) = FraudSignal(
        code = FraudSignalCode.IBAN_COUNTRY_MISMATCH,
        severity = FraudSignalSeverity.CHECK,
        iban = iban,
        creditorCountry = country,
    )

    private fun ibanCandidates(vararg candidates: String) = mapOf(
        "iban" to GiniCaptureSpecificExtraction(
            "iban",
            candidates.first(),
            "iban",
            null,
            candidates.map { GiniCaptureExtraction(it, "iban", null) },
        )
    )

    private fun recipient(name: String) = mapOf("paymentRecipient" to text("paymentRecipient", name))

    private fun text(name: String, value: String) =
        GiniCaptureSpecificExtraction(name, value, "text", null, emptyList())
}
