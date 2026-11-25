package net.gini.android.capture.analysis.fraud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IBANSelectionArgsTest {

    @Test
    fun `QR vs printed offers the QR IBAN and every printed IBAN in order`() {
        val selection = args(IBANMismatchType.QR_VS_PRINTED).toSelectionArgs()

        assertThat(selection).isEqualTo(IBANSelectionArgs(qrIban = QR, printedIbans = PRINTED))
    }

    @Test
    fun `combined offers the same IBANs as QR vs printed`() {
        val selection = args(IBANMismatchType.COMBINED).toSelectionArgs()

        assertThat(selection).isEqualTo(IBANSelectionArgs(qrIban = QR, printedIbans = PRINTED))
    }

    @Test
    fun `country offers nothing to choose, even with a QR IBAN`() {
        assertThat(args(IBANMismatchType.COUNTRY).toSelectionArgs()).isNull()
    }

    @Test
    fun `without a QR IBAN there is nothing to choose`() {
        assertThat(args(IBANMismatchType.QR_VS_PRINTED, qrIban = null).toSelectionArgs()).isNull()
        assertThat(args(IBANMismatchType.COMBINED, qrIban = null).toSelectionArgs()).isNull()
    }

    @Test
    fun `without printed IBANs there is nothing to choose`() {
        assertThat(args(IBANMismatchType.QR_VS_PRINTED, printedIbans = emptyList()).toSelectionArgs())
            .isNull()
        assertThat(args(IBANMismatchType.COMBINED, printedIbans = emptyList()).toSelectionArgs())
            .isNull()
    }

    private fun args(
        type: IBANMismatchType,
        qrIban: IBANMismatchIban? = QR,
        printedIbans: List<IBANMismatchIban> = PRINTED,
    ) = IBANMismatchArgs(type = type, qrIban = qrIban, printedIbans = printedIbans)

    private companion object {
        val QR = IBANMismatchIban("PL61109010140000071219812874", hintCountryCode = "PL")
        val PRINTED = listOf(
            IBANMismatchIban("DE89370400440532013000"),
            IBANMismatchIban("AT611904300234573201", hintCountryCode = "AT"),
        )
    }
}
