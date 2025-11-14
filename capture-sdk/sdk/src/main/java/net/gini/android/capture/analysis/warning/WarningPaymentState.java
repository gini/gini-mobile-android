package net.gini.android.capture.analysis.warning;

import androidx.annotation.Nullable;


/**
 * Represents the normalized payment state of a document (e.g. invoice, receipt)
 * extracted during analysis.
 *
 * Responsibilities:
 * - Encapsulates the set of supported payment states that the app cares about
 *   (PAID, TO_BE_PAID, BOOKED, UNKNOWN).
 * - Provides a safe conversion from raw string values (via {@link #from(String)})
 *   into one of the enum constants, handling nulls and unexpected values gracefully.
 * - Offers convenience methods (e.g. {@link #isPaid()}) to simplify
 *   conditional checks in presenters or views.
 */
 public enum WarningPaymentState {
    PAID, TO_BE_PAID, BOOKED, UNKNOWN;

    public static WarningPaymentState from(@Nullable String raw) {
        if (raw == null) return UNKNOWN;
        switch (raw.trim()) {
            case "Paid":      return PAID;
            case "ToBePaid":  return TO_BE_PAID;
            case "booked":    return BOOKED;
            default:          return UNKNOWN;
        }
    }

    /**
     * Checks if the payment state indicates that the document is already paid.
     *
     * @return true if the payment state is {@link #PAID}, false otherwise.
     */
    public boolean isPaid() { return this == PAID; }

    /**
     * Checks if the payment state indicates that the document is to be paid.
     *
     * @return true if the payment state is {@link #TO_BE_PAID}, false otherwise.
     */
    public boolean toBePaid() { return this == TO_BE_PAID; }
}
