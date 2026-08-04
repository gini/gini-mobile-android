package net.gini.android.capture.analysis.warning;

import androidx.annotation.StringRes;

import net.gini.android.capture.R;

/**
 * Represents the different warning scenarios that can be shown in the UI
 * (e.g. inside {@link WarningBottomSheet}).
 * Each enum value holds the resource IDs for its title, description and the labels of
 * the primary CTA (filled, first button) and secondary CTA (outlined, second button),
 * so that the UI can easily fetch localized strings when displaying the warning.
 * The behavior behind the two CTAs is wired by the caller showing the sheet.
 */
public enum WarningType {
    DOCUMENT_MARKED_AS_PAID(
            R.string.gc_document_marked_paid_title,
            R.string.gc_document_marked_paid_desc,
            R.string.gc_cancel_transfer,
            R.string.gc_proceed_anyway
    ),
    PAYMENT_DUE_DATE(
            R.string.gc_due_date_hint_title,
            R.string.gc_due_date_hint_desc,
            R.string.gc_proceed_anyway,
            R.string.gc_cancel_transfer
    );

    @StringRes private final int titleRes;
    @StringRes private final int descriptionRes;
    @StringRes private final int primaryButtonTextRes;
    @StringRes private final int secondaryButtonTextRes;

    WarningType(@StringRes int titleRes, @StringRes int descriptionRes,
                @StringRes int primaryButtonTextRes, @StringRes int secondaryButtonTextRes) {
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.primaryButtonTextRes = primaryButtonTextRes;
        this.secondaryButtonTextRes = secondaryButtonTextRes;
    }
    public int getTitleRes() { return titleRes; }
    public int getDescriptionRes() { return descriptionRes; }
    public int getPrimaryButtonTextRes() { return primaryButtonTextRes; }
    public int getSecondaryButtonTextRes() { return secondaryButtonTextRes; }
}
