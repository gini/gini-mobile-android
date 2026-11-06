package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN selection sheet: the two IBANs the user chooses between after
 * "Proceed anyway" on the IBAN mismatch warning. Every text arrives already resolved; the sheet
 * only lays it out.
 *
 * @property qrIban the IBAN read from the payment QR code
 * @property printedIban the IBAN printed on the document
 * @property qrIbanCountry the QR IBAN's country when it differs from the sender's; shown as the
 * red hint below that IBAN
 * @property printedIbanCountry the printed IBAN's country when it differs from the sender's
 */
internal data class IBANSelectionArgs(
    val qrIban: String,
    val printedIban: String,
    val qrIbanCountry: String? = null,
    val printedIbanCountry: String? = null,
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
                qrIbanCountry = qrIbanCountry,
                printedIbanCountry = printedIbanCountry,
            )
        }
