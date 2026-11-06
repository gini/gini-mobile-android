package net.gini.android.capture.analysis.fraud

/**
 * Display data of the IBAN mismatch warning. The screen resolves every sentence from string
 * resources, so banks can override them; whoever builds an instance from an analysis result
 * supplies only the values that go into them. Country names arrive as localised display names
 * (e.g. `Locale.getDisplayCountry`), never as ISO codes.
 *
 * @property type which signals the screen explains
 * @property qrIban the IBAN read from the payment QR code, `null` when only the country differs
 * @property printedIban the IBAN printed on the document
 * @property senderName the sender's name, shown when the country signal is present
 * @property senderCountry the sender's country, shown next to [senderName]
 * @property qrIbanCountry the QR IBAN's country when it differs from the sender's; shown as the
 * red hint below that IBAN
 * @property printedIbanCountry the printed IBAN's country when it differs from the sender's
 */
internal data class IBANMismatchArgs(
    val type: IBANMismatchType,
    val qrIban: String?,
    val printedIban: String,
    val senderName: String? = null,
    val senderCountry: String? = null,
    val qrIbanCountry: String? = null,
    val printedIbanCountry: String? = null,
)
