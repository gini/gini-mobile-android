package net.gini.android.capture.fraudSignals

/**
 * One fraud signal read from the `fraudSignals` compound extraction.
 *
 * @property iban the IBAN delivered for payment (same value as the `iban` extraction)
 * @property creditorCountry ISO 3166-1 alpha-2 country of the payment recipient; always set for
 * [FraudSignalCode.IBAN_COUNTRY_MISMATCH], optional for the other codes
 */
internal data class FraudSignal(
    val code: FraudSignalCode,
    val severity: FraudSignalSeverity,
    val iban: String,
    val creditorCountry: String?,
)

internal enum class FraudSignalCode {
    /** The IBAN in the payment QR code matches none of the IBANs printed on the document. */
    QR_PRINTED_IBAN_MISMATCH,

    /** The country of the delivered IBAN differs from the payment recipient's country. */
    IBAN_COUNTRY_MISMATCH;

    companion object {
        /** Returns `null` for codes this SDK version does not know. */
        fun fromApiValue(value: String?): FraudSignalCode? = entries.firstOrNull { it.name == value }
    }
}

internal enum class FraudSignalSeverity {
    STOP,
    CHECK,
    ESCALATOR,
    SILENT;

    companion object {
        /** Returns `null` for severities this SDK version does not know. */
        fun fromApiValue(value: String?): FraudSignalSeverity? =
            entries.firstOrNull { it.name == value }
    }
}
