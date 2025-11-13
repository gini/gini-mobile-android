package net.gini.android.capture.analysis.warning;

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
            R.string.gc_document_marked_paid_desc
    );

    @StringRes private final int titleRes;
    @StringRes private final int descriptionRes;

    WarningType(@StringRes int titleRes, @StringRes int descriptionRes) {
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
    }
    public int getTitleRes() { return titleRes; }
    public int getDescriptionRes() { return descriptionRes; }
}
