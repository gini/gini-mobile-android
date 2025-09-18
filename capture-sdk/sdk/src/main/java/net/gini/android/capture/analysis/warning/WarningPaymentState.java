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
            case "ToBePaid":  return TO_BE_PAID; // in future versions this might be "ToBePaid"
            case "booked":    return BOOKED; // in future versions this might be "Booked"
            default:          return UNKNOWN;
        }
    }

    public boolean isPaid() { return this == PAID; }
}
