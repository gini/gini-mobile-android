package net.gini.android.capture.analysis.fraud

/**
 * Which fraud signals the IBAN mismatch warning explains. Selects the title and description of
 * the screen; the IBAN rows come from [IbanMismatchArgs].
 */
internal enum class IbanMismatchType {
    /** The IBAN in the payment QR code differs from the IBAN printed on the document. */
    QR_VS_PRINTED,

    /** The country of the IBAN on the document differs from the sender's country. */
    COUNTRY,

    /** Both signals at once, shown as one combined screen. */
    COMBINED,
}
