package net.gini.android.capture.analysis.fraud

import androidx.annotation.RestrictTo
import java.util.Locale

/**
 * Development switch for the IBAN mismatch warning, in the spirit of
 * [net.gini.android.capture.GiniCaptureDebug]: while [case] is set the Analysis screen shows the
 * warning for that case with [sampleArgs] after every successful analysis, ahead of the other
 * warnings, so every layout of the screen can be exercised without a document that carries the
 * fraud signals.
 *
 * Off by default and not reset by `GiniCapture.cleanup()`. Public because the example app is a
 * separate Gradle module and Kotlin `internal` does not cross module boundaries; integrators
 * have no reason to call it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IBANMismatchDebug {

    /** The warning layouts the switch can open, one per [IBANMismatchType]. */
    enum class Case { QR_VS_PRINTED, COUNTRY, COMBINED }

    /**
     * The case the Analysis screen shows before handing the extractions on; `null` turns the
     * switch off.
     */
    @JvmStatic
    var case: Case? = null

    /** The design's sample data for [case]. */
    internal fun sampleArgs(case: Case): IBANMismatchArgs = when (case) {
        Case.QR_VS_PRINTED -> IBANMismatchArgs(
            type = IBANMismatchType.QR_VS_PRINTED,
            qrIban = SAMPLE_QR_IBAN,
            printedIban = SAMPLE_PRINTED_IBAN,
        )

        Case.COUNTRY -> IBANMismatchArgs(
            type = IBANMismatchType.COUNTRY,
            qrIban = null,
            printedIban = SAMPLE_QR_IBAN,
            senderName = SAMPLE_SENDER_NAME,
            senderCountry = displayCountry(SAMPLE_SENDER_COUNTRY_CODE),
            printedIbanCountry = displayCountry(SAMPLE_QR_IBAN_COUNTRY_CODE),
        )

        Case.COMBINED -> IBANMismatchArgs(
            type = IBANMismatchType.COMBINED,
            qrIban = SAMPLE_QR_IBAN,
            printedIban = SAMPLE_PRINTED_IBAN,
            senderName = SAMPLE_SENDER_NAME,
            senderCountry = displayCountry(SAMPLE_SENDER_COUNTRY_CODE),
            qrIbanCountry = displayCountry(SAMPLE_QR_IBAN_COUNTRY_CODE),
        )
    }

    /** The localised country name for an ISO 3166 code, as the fraud signals will provide it. */
    private fun displayCountry(isoCode: String): String =
        Locale.Builder().setRegion(isoCode).build().displayCountry

    private const val SAMPLE_QR_IBAN = "PL12100001110100100012345678"
    private const val SAMPLE_QR_IBAN_COUNTRY_CODE = "PL"
    private const val SAMPLE_PRINTED_IBAN = "DE89370400440532013000"
    private const val SAMPLE_SENDER_NAME = "Elektro Brandt GmbH"
    private const val SAMPLE_SENDER_COUNTRY_CODE = "DE"
}
