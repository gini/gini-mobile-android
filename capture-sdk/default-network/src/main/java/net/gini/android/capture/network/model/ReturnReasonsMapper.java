package net.gini.android.capture.network.model;

import net.gini.android.core.api.models.ReturnReason;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Helper class to map the {@link net.gini.android.models.ReturnReason} from the Gini API SDK to the Gini Capture
 * Library's {@link GiniCaptureReturnReason} and vice versa.
 */
public class ReturnReasonsMapper {

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