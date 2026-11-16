package net.gini.android.capture.analysis.fraud

import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Writes the IBAN the user chose on the [IBANSelectionSheet] into the specific extractions, so
 * the bank receives the chosen IBAN instead of the backend's, which is the QR code IBAN.
 */
internal object ChosenIbanExtractions {

    private const val IBAN_KEY = "iban"
    private const val BIC_KEY = "bic"

    /**
     * [extractions] with the `iban` extraction replaced by [chosenIban]: no box, because the
     * backend's box marks the QR code IBAN; the backend's candidates; marked dirty, because the
     * user changed the value. The `bic` extraction is removed, because it belongs to the
     * backend's IBAN and the response does not say which BIC belongs to the chosen one; banks
     * derive the BIC from the IBAN. Returns [extractions] itself when there is nothing to
     * change — no choice, or the chosen IBAN equals the backend's. Never throws.
     */
    fun apply(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        chosenIban: String?,
    ): Map<String, GiniCaptureSpecificExtraction> {
        val current = extractions[IBAN_KEY]
        return if (
            chosenIban.isNullOrBlank() ||
            (current != null && normalizeIban(current.value) == normalizeIban(chosenIban))
        ) {
            extractions
        } else {
            val chosen = GiniCaptureSpecificExtraction(
                IBAN_KEY,
                chosenIban,
                IBAN_KEY,
                null,
                current?.candidates.orEmpty(),
            ).apply { setIsDirty(true) }
            extractions - BIC_KEY + (IBAN_KEY to chosen)
        }
    }
}
