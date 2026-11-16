package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN selection sheet: the IBANs the user chooses between after
 * "Proceed anyway" on the IBAN mismatch warning. The sheet resolves every text from string
 * resources, like the warning; it lists the same IBANs as the warning, in the same order.
 *
 * @property qrIban the IBAN read from the payment QR code
 * @property printedIbans the IBANs printed on the document, in document order; never empty
 */
internal data class IBANSelectionArgs(
    val qrIban: IBANMismatchIban,
    val printedIbans: List<IBANMismatchIban>,
)

/**
 * The selection the warning offers after "Proceed anyway", or `null` when there is nothing to
 * choose: the country-only case has a single IBAN and continues with it directly.
 */
internal fun IBANMismatchArgs.toSelectionArgs(): IBANSelectionArgs? =
    qrIban
        ?.takeIf { type != IBANMismatchType.COUNTRY && printedIbans.isNotEmpty() }
        ?.let { qrIban -> IBANSelectionArgs(qrIban = qrIban, printedIbans = printedIbans) }
