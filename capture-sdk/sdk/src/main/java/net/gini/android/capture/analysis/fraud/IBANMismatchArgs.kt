package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN mismatch warning, built from the fraud signals by
 * [IBANMismatchArgsMapper]. It carries values and country codes only; the screen resolves every
 * text from string resources, so banks can override them.
 *
 * @property type which signals the screen explains
 * @property qrIban the IBAN read from the payment QR code, `null` when only the country differs
 * @property printedIbans the IBANs printed on the document, in document order; in the
 * country-only case exactly the IBAN delivered for payment
 * @property senderName the payment recipient, `null` when the document has none
 * @property senderCountryCode ISO 3166-1 alpha-2 country of the sender; non-null only when the
 * country signal is present, and then the sender line is shown
 */
internal data class IBANMismatchArgs(
    val type: IBANMismatchType,
    val qrIban: IBANMismatchIban?,
    val printedIbans: List<IBANMismatchIban>,
    val senderName: String? = null,
    val senderCountryCode: String? = null,
)

/**
 * One IBAN on the warning.
 *
 * @property hintCountryCode ISO 3166-1 alpha-2 country of [iban] when it differs from the
 * sender's country; non-null shows the "From <country>, not the sender's country" hint
 */
internal data class IBANMismatchIban(
    val iban: String,
    val hintCountryCode: String? = null,
)
