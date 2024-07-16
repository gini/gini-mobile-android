package net.gini.android.bank.sdk.capture.skonto

/**
 * Exceptions related to the Skonto feature.
 */
sealed class SkontoException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Internal use only.
     *
     * @suppress
     */
    class SkontoMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class DueDateMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class AmountDiscountedMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class AmountToPayMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class RemainingDaysMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)

    /**
     * Internal use only.
     *
     * @suppress
     */
    class PaymentMethodMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)


    /**
     * Internal use only.
     *
     * @suppress
     */
    class PercentageDiscountedMissingException(message: String? = null, cause: Throwable? = null) : SkontoException(message, cause)
}