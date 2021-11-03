package net.gini.android.capture.example.shared;

import android.content.Intent;
import android.os.Bundle;

import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ExampleUtil {

    public static boolean isIntentActionViewOrSend(@NonNull final Intent intent) {
        final String action = intent.getAction();
        return Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action);
    }

    public static boolean hasNoPay5Extractions(final Set<String> extractionNames) {
        for (final String extractionName : extractionNames) {
            if (isPay5Extraction(extractionName)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPay5Extraction(final String extractionName) {
        return extractionName.equals("amountToPay") ||
                extractionName.equals("bic") ||
                extractionName.equals("iban") ||
                extractionName.equals("paymentReference") ||
                extractionName.equals("paymentRecipient");
    }

    public static Bundle getExtractionsBundle(
            @Nullable final Map<String, GiniCaptureSpecificExtraction> extractions) {
        if (extractions == null) {
            return null;
        }
        final Bundle extractionsBundle = new Bundle();
        for (final Map.Entry<String, GiniCaptureSpecificExtraction> entry : extractions.entrySet()) {
            extractionsBundle.putParcelable(entry.getKey(), entry.getValue());
        }
        return extractionsBundle;
    }

    public static Bundle getLegacyExtractionsBundle(
            @Nullable final Map<String, SpecificExtraction> extractions) {
        if (extractions == null) {
            return null;
        }
        final Bundle extractionsBundle = new Bundle();
        for (final Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
            extractionsBundle.putParcelable(entry.getKey(), entry.getValue());
        }
        return extractionsBundle;
    }

    private ExampleUtil() {
    }
}
