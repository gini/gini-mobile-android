package net.gini.android.capture.analysis.fraud

import androidx.annotation.RestrictTo

/**
 * Debug toggle for the PP-3941 spike: forces the IBAN mismatch modal after every successful
 * analysis with hardcoded sample IBANs.
 *
 * Throwaway code on the spike branch — never merged. It is public only because the example app
 * is a separate Gradle module and Kotlin `internal` does not cross module boundaries.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IbanMismatchSpike {

    /** Log tag for the selected IBAN; see the spec's R6. */
    const val LOG_TAG: String = "IbanMismatchSpike"

    /** Sample IBAN as if read from the payment QR code. */
    const val SAMPLE_QR_IBAN: String = "DE02120300000000202051"

    /** Sample IBAN as if printed on the invoice. */
    const val SAMPLE_PRINTED_IBAN: String = "DE02500105170137075030"

    /** When `true` the Analysis screen shows the mismatch modal before handing extractions on. */
    @JvmStatic
    var enabled: Boolean = true
}
