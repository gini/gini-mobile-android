package net.gini.android.capture.analysis.warning;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.gini.android.capture.R;

/**
 * Represents the different warning scenarios that can be shown in the UI
 * (e.g. inside {@link WarningBottomSheet}).
 * Each enum value holds the resource IDs for its title and description,
 * so that the UI can easily fetch localized strings when displaying the warning.
 */
public enum WarningType {
    DOCUMENT_MARKED_AS_PAID(
            R.string.gc_document_marked_paid_title,
            R.string.gc_document_marked_paid_desc,
            R.drawable.gc_bg_warning_circle
    ), DOCUMENT_MARKED_AS_CREDIT_NOTE(
            R.string.gc_document_marked_credit_note_title,
            R.string.gc_document_marked_credit_note_desc,
            R.drawable.gc_warning_icon
    );

    @StringRes
    private final int titleRes;
    @StringRes
    private final int descriptionRes;
    @DrawableRes
    private final int iconRes;

    WarningType(@StringRes int titleRes, @StringRes int descriptionRes, @DrawableRes int iconRes) {
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.iconRes = iconRes;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getDescriptionRes() {
        return descriptionRes;
    }

    public int getIconRes() {
        return iconRes;
    }
}
