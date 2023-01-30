package net.gini.android.bank.sdk.capture.digitalinvoice.help

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.gini.android.bank.sdk.R

enum class HelpItem(@DrawableRes val drawableResource: Int, @StringRes val textResource: Int, @StringRes val titleTextResource: Int) {
    DIGITAL_INVOICE(R.drawable.gbs_help_question_icon, R.string.gbs_help_invoice_text, R.string.gbs_help_invoice_title),
    EDIT(R.drawable.gbs_help_edit_icon, R.string.gbs_help_edit_text, R.string.gbs_help_edit_title),
    SHOP(R.drawable.gbs_help_shop_icon, R.string.gbs_help_online_shops_text, R.string.gbs_help_online_shops_title)
}
