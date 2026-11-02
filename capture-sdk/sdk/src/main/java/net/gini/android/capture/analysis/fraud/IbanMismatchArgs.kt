package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN mismatch warning. Every text that depends on the analysed document
 * arrives here already resolved; the screen only lays it out. Whoever builds an instance from an
 * analysis result must resolve [sender], [qrIbanHint] and [printedIbanHint] from string
 * resources (with the country as a placeholder), never from inline literals, so banks can
 * override them.
 *
 * @property type which signals the screen explains
 * @property qrIban the IBAN read from the payment QR code, `null` when only the country differs
 * @property printedIban the IBAN printed on the document
 * @property sender the sender's name and country, shown when the country signal is present
 * @property qrIbanHint explanation below the QR IBAN, e.g. "From Poland, not the sender's country"
 * @property printedIbanHint explanation below the printed IBAN
 */
internal data class IbanMismatchArgs(
    val type: IbanMismatchType,
    val qrIban: String?,
    val printedIban: String,
    val sender: String? = null,
    val qrIbanHint: String? = null,
    val printedIbanHint: String? = null,
)
