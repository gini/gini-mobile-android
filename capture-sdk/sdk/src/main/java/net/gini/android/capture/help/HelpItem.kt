package net.gini.android.capture.help

import android.content.Intent
import androidx.annotation.StringRes
import net.gini.android.capture.R

/**
 * This sealed class declares the items which can be shown in the Help Screen.
 */
sealed class HelpItem(
    @StringRes val title: Int
) {

    /**
     * Shows tips for taking better pictures.
     *
     * Item label customizable by overriding the string resource named {@code gc_help_item_photo_tips_title}
     */
    object PhotoTips : HelpItem(
        R.string.gc_help_item_photo_tips_title
    )

    /**
     * Shows a guide for importing files from other apps via "open with".
     *
     * Item label customizable by overriding the string resource named {@code gc_help_item_file_import_guide_title}
     */
    object FileImport : HelpItem(
        R.string.gc_help_item_file_import_guide_title
    )

    /**
     * Shows information about the document formats supported by the Gini Capture SDK.
     *
     * Item label customizable by overriding the string resource named {@code gc_help_item_supported_formats_title}
     */
    object SupportedFormats : HelpItem(
        R.string.gc_help_item_supported_formats_title
    )

    /**
     * Shows a custom help item with the given title and starts an activity with the given intent when clicked.
     */
    data class Custom(val customTitle: Int, val intent: Intent) : HelpItem(customTitle)
}