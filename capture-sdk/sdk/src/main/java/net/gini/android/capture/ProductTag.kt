package net.gini.android.capture

/**
 * Product tag to identify which extraction type to use.
 *
 *
 * @property value The string value of the product tag
 */
sealed class ProductTag(val value: String) {

    /**
     * SEPA extractions - shows normal extractions.
     * This is the default behavior.
     */
    object SepaExtractions : ProductTag("sepa-extractions")

    /**
     * Cross-border extractions - shows compound extractions.
     */
    object CxExtractions : ProductTag("cx-extractions")

    /**
     * Auto-detect extractions.
     *
     * Note: This option is reserved for future use and is not yet available for customer use.
     */
    object AutoDetectExtractions : ProductTag("auto-detect-extractions")

    /**
     * Custom product tag for extensibility.
     *
     * @property customValue The custom product tag value
     */
    data class OtherProductTag(val customValue: String) : ProductTag(customValue)
}
