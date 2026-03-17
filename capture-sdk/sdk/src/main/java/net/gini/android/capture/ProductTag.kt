package net.gini.android.capture

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Product tag to identify which extraction type to use.
 *
 *
 * @property value The string value of the product tag
 */
@Parcelize
sealed class ProductTag(val value: String) : Parcelable {

    /**
     * SEPA extractions - shows normal extractions.
     * This is the default behavior.
     */
    @Parcelize
    object SepaExtractions : ProductTag("sepa-extractions")

    /**
     * Cross-border extractions - shows compound extractions.
     */
    @Parcelize
    object CxExtractions : ProductTag("cx-extractions")

    /**
     * Auto-detect extractions.
     *
     * Note: This option is reserved for future use and is not yet available for customer use.
     */
    @Parcelize
    object AutoDetectExtractions : ProductTag("auto-detect-extractions")

    /**
     * Custom product tag for extensibility.
     *
     * @property customValue The custom product tag value
     */
    @Parcelize
    data class OtherProductTag(val customValue: String) : ProductTag(customValue)
}
