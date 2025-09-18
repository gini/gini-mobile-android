package net.gini.android.capture.analysis.warning;

import androidx.annotation.StringRes;

import net.gini.android.capture.R;

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
