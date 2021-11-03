package net.gini.android.capture.network.model;

import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.SpecificExtraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Helper class to map the {@link CompoundExtraction} from the Gini API SDK to the Gini Capture
 * Library's {@link GiniCaptureCompoundExtraction} and vice versa.
 */
public final class CompoundExtractionsMapper {

    @NonNull
    public static Map<String, GiniCaptureCompoundExtraction> mapToGiniCapture(@NonNull final Map<String, CompoundExtraction> sourceMap) {
        final Map<String, GiniCaptureCompoundExtraction> targetMap = new HashMap<>(sourceMap.size());
        for (final Map.Entry<String, CompoundExtraction> source : sourceMap.entrySet()) {
            targetMap.put(source.getKey(), toGiniCapture(source.getValue()));
        }
        return targetMap;
    }

    private static GiniCaptureCompoundExtraction toGiniCapture(@NonNull final CompoundExtraction source) {
        return new GiniCaptureCompoundExtraction(source.getName(), mapListToGiniCapture(source.getSpecificExtractionMaps()));
    }

    @NonNull
    private static List<Map<String, GiniCaptureSpecificExtraction>> mapListToGiniCapture(final List<Map<String, SpecificExtraction>> sourceList) {
        final List<Map<String, GiniCaptureSpecificExtraction>> targetList = new ArrayList<>(sourceList.size());
        for (final Map<String, SpecificExtraction> sourceMap : sourceList) {
            targetList.add(SpecificExtractionMapper.mapToGiniCapture(sourceMap));
        }
        return targetList;
    }

    @NonNull
    public static Map<String, CompoundExtraction> mapToApiSdk(@NonNull final Map<String, GiniCaptureCompoundExtraction> sourceMap) {
        final Map<String, CompoundExtraction> targetMap = new HashMap<>(sourceMap.size());
        for (final Map.Entry<String, GiniCaptureCompoundExtraction> source : sourceMap.entrySet()) {
            targetMap.put(source.getKey(), toApiSdk(source.getValue()));
        }
        return targetMap;
    }

    private static CompoundExtraction toApiSdk(@NonNull final GiniCaptureCompoundExtraction source) {
        return new CompoundExtraction(source.getName(), mapListToApiSdk(source.getSpecificExtractionMaps()));
    }

    @NonNull
    private static List<Map<String, SpecificExtraction>> mapListToApiSdk(final List<Map<String, GiniCaptureSpecificExtraction>> sourceList) {
        final List<Map<String, SpecificExtraction>> targetList = new ArrayList<>(sourceList.size());
        for (final Map<String, GiniCaptureSpecificExtraction> sourceMap : sourceList) {
            targetList.add(SpecificExtractionMapper.mapToApiSdk(sourceMap));
        }
        return targetList;
    }

    private CompoundExtractionsMapper() {
    }
}
