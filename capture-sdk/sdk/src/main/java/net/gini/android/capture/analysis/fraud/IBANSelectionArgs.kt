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

/**
 * The selection the warning offers after "Proceed anyway", or `null` when there is nothing to
 * choose: the country-only case has a single IBAN and continues with it directly.
 */
internal fun IBANMismatchArgs.toSelectionArgs(): IBANSelectionArgs? =
    qrIban
        ?.takeIf { type != IBANMismatchType.COUNTRY }
        ?.let { qrIban ->
            IBANSelectionArgs(
                qrIban = qrIban,
                printedIban = printedIban,
                qrIbanHint = qrIbanHint,
                printedIbanHint = printedIbanHint,
            )
        }
