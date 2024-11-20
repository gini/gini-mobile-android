package net.gini.android.internal.payment.review

import net.gini.android.internal.payment.review.reviewComponent.ReviewFields


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

    /**
     * If set to `true`, the [Amount] field will be editable.
     * If set to `false` the [Amount] field will be read-only.
     *
     * Default value is `true`
     */
    internal val editableFields: List<ReviewFields> =
        listOf(ReviewFields.IBAN, ReviewFields.AMOUNT, ReviewFields.RECIPIENT, ReviewFields.PURPOSE),

    /**
     * If set to `true`, the small [BankSelectionButton] will be shown on the [ReviewView].
     * If set to `false`, only the `To the banking app` button will be displayed.
     *
     * Default value is `true`
     */
    internal val selectBankButtonVisible: Boolean = true
)