package net.gini.android.bank.sdk.capture.digitalinvoice

/**
 * Created by Alpar Szotyori on 10.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * Exceptions related to the digital invoice used in the return assistant feature.
 */
sealed class DigitalInvoiceException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Internal use only.
     *
     * @suppress
     */
    class LineItemsMissingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class DescriptionMissingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class QuantityMissingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class GrossPriceMissingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class ArticleNumberMissingException(message: String? = null, cause: Throwable? = null) :
            DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class MixedCurrenciesException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class QuantityParsingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class GrossPriceParsingException(message: String? = null, cause: Throwable? = null) : DigitalInvoiceException(message, cause)
}