package net.gini.android.capture.analysis.warning;

import androidx.annotation.Nullable;


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
