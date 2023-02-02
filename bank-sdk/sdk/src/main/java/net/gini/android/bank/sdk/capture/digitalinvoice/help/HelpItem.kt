package net.gini.android.bank.sdk.capture.digitalinvoice.help

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.gini.android.bank.sdk.R

/**
 * Internal use only.
 * Custom enum class to gather all resources needed to show help tips.
 *
 */
enum class HelpItem(@DrawableRes val drawableResource: Int, @StringRes val textResource: Int, @StringRes val titleTextResource: Int,
                    @StringRes val iconContentTextResource: Int) {
    DIGITAL_INVOICE(R.drawable.gbs_help_question_icon, R.string.gbs_help_invoice_text,
        R.string.gbs_help_invoice_title, R.string.gbs_digital_invoice_help_invoice_icon_content_description),
    EDIT(R.drawable.gbs_help_edit_icon, R.string.gbs_help_edit_text,
        R.string.gbs_help_edit_title, R.string.gbs_digital_invoice_help_edit_icon_content_description),
    SHOP(R.drawable.gbs_help_shop_icon, R.string.gbs_help_online_shops_text,
        R.string.gbs_help_online_shops_title, R.string.gbs_digital_invoice_help_shop_icon_content_description)
}
