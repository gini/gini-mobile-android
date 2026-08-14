package net.gini.android.capture.analysis.warning;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

import net.gini.android.capture.R;

/**
 * Represents the different warning scenarios that can be shown in the UI
 * (e.g. inside {@link WarningBottomSheet}).
 * Each enum value holds the resource IDs for its title, description and the labels of
 * the primary CTA (filled, first button) and secondary CTA (outlined, second button),
 * so that the UI can easily fetch localized strings when displaying the warning.
 * The behavior behind the two CTAs is wired by the caller showing the sheet.
 * Types whose title contains a format placeholder declare it via
 * {@code requiresTitleFormatArg}, so the sheet can fail loudly when the argument is missing
 * instead of rendering a literal placeholder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public enum WarningType {
    DOCUMENT_MARKED_AS_PAID(
            R.string.gc_document_marked_paid_title,
            R.string.gc_document_marked_paid_desc,
            R.string.gc_cancel_transfer,
            R.string.gc_proceed_anyway,
            false
    ),
    PAYMENT_DUE_DATE(
            R.string.gc_due_date_hint_title,
            R.string.gc_due_date_hint_desc,
            R.string.gc_proceed_anyway,
            R.string.gc_cancel_transfer,
            true
    ),
    /**
     * Scheduled payment state of the due date bottom sheet. Shares its title with
     * {@link #PAYMENT_DUE_DATE} (both read "Your invoice is due on &lt;date&gt;.") and offers
     * "Schedule Payment" as the primary CTA, which hands the extractions back to the bank app
     * instead of continuing the pay-now flow.
     */
    SCHEDULE_PAYMENT(
            R.string.gc_due_date_hint_title,
            R.string.gc_schedule_payment_hint_desc,
            R.string.gc_schedule_payment,
            R.string.gc_proceed_anyway,
            true
    );

    @StringRes private final int titleRes;
    @StringRes private final int descriptionRes;
    @StringRes private final int primaryButtonTextRes;
    @StringRes private final int secondaryButtonTextRes;
    private final boolean requiresTitleFormatArg;

    WarningType(@StringRes int titleRes, @StringRes int descriptionRes,
                @StringRes int primaryButtonTextRes, @StringRes int secondaryButtonTextRes,
                boolean requiresTitleFormatArg) {
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.primaryButtonTextRes = primaryButtonTextRes;
        this.secondaryButtonTextRes = secondaryButtonTextRes;
        this.requiresTitleFormatArg = requiresTitleFormatArg;
    }
    public int getTitleRes() { return titleRes; }
    public int getDescriptionRes() { return descriptionRes; }
    public int getPrimaryButtonTextRes() { return primaryButtonTextRes; }
    public int getSecondaryButtonTextRes() { return secondaryButtonTextRes; }
    public boolean requiresTitleFormatArg() { return requiresTitleFormatArg; }
}
