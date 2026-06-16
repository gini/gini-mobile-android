package net.gini.android.capture.network.model;

import androidx.annotation.NonNull;

import net.gini.android.bank.api.models.ReturnReason;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to map the {@link net.gini.android.bank.api.models.ReturnReason} from the Gini API SDK to the Gini Capture
 * Library's {@link GiniCaptureReturnReason} and vice versa.
 */
public final class ReturnReasonsMapper {

    private ReturnReasonsMapper() {
        // Utility class - do not instantiate
    }

    public static List<ReturnReason> mapToApiSdk(@NonNull final List<GiniCaptureReturnReason> sourceList) {
        final List<ReturnReason> targetList = new ArrayList<>(sourceList.size());
        for (final GiniCaptureReturnReason source : sourceList) {
            targetList.add(new ReturnReason(source.getId(), source.getLocalizedLabels()));
        }
        return targetList;
    }

    public static List<GiniCaptureReturnReason> mapToGiniCapture(@NonNull final List<ReturnReason> sourceList) {
        final List<GiniCaptureReturnReason> targetList = new ArrayList<>(sourceList.size());
        for (final ReturnReason source : sourceList) {
            targetList.add(new GiniCaptureReturnReason(source.getId(), source.getLocalizedLabels()));
        }
        return targetList;
    }
}