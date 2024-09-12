package net.gini.android.internal.payment.review


/**
 * Configuration for the [ReviewBottomSheet].
 */
data class ReviewConfiguration(
    /**
     * If set to `true`, the [ReviewBottomSheet] will handle errors internally and show snackbars for errors.
     * If set to `false`, errors will be ignored by the [ReviewBottomSheet]. In this case the flows exposed by [GiniMerchant] should be observed for errors.
     *
     * Default value is `true`.
     */
    val handleErrorsInternally: Boolean = true,

    //todo take this out - only applicable in health
    /**
     * Set to `true` to show a close button. Set a [ReviewFragmentListener] to be informed when the
     * button is pressed.
     *
     * Default value is `false`.
     */
    val showCloseButton: Boolean = false,

    //todo refactor to include other fields
    /**
     * If set to `true`, the [Amount] field will be editable.
     * If set to `false` the [Amount] field will be read-only.
     *
     * Default value is `true`
     */
    internal val isAmountFieldEditable: Boolean = true
)