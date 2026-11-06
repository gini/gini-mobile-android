package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN selection sheet: the two IBANs the user chooses between after
 * "Proceed anyway" on the IBAN mismatch warning. Every text arrives already resolved; the sheet
 * only lays it out.
 *
 * @property qrIban the IBAN read from the payment QR code
 * @property printedIban the IBAN printed on the document
 * @property qrIbanHint explanation below the QR IBAN, e.g. "From Poland, not the sender's country"
 * @property printedIbanHint explanation below the printed IBAN
 */
internal data class IBANSelectionArgs(
    val qrIban: String,
    val printedIban: String,
    val qrIbanHint: String? = null,
    val printedIbanHint: String? = null,
)
