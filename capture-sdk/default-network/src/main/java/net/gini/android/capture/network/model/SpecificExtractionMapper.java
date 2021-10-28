package net.gini.android.capture.network.model;

import net.gini.android.core.api.models.SpecificExtraction;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 30.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Helper class to map the {@link SpecificExtraction} from the Gini Bank API lib to the Gini Capture
 * SDK's {@link GiniCaptureSpecificExtraction} and vice versa.
 */
public final class SpecificExtractionMapper {

    /**
     * Convert a map of {@link SpecificExtraction}s from the Gini Bank API lib to a map of Gini Capture
     * SDK {@link GiniCaptureSpecificExtraction}s.
     *
     * @param sourceMap map of Gini Bank API lib {@link SpecificExtraction}s
     *
     * @return map of Gini Capture SDK {@link GiniCaptureSpecificExtraction}s
     */
    @NonNull
    public static Map<String, GiniCaptureSpecificExtraction> mapToGiniCapture(
            @NonNull final Map<String, SpecificExtraction> sourceMap) {
        final Map<String, GiniCaptureSpecificExtraction> targetMap = new HashMap<>(sourceMap.size());
        for (final Map.Entry<String, SpecificExtraction> source : sourceMap.entrySet()) {
            targetMap.put(source.getKey(), map(source.getValue()));
        }
        return targetMap;
    }

    /**
     * Map a {@link SpecificExtraction} from the Gini Bank API lib to the Gini Capture SDK's {@link
     * GiniCaptureSpecificExtraction}.
     *
     * @param source Gini Bank API lib {@link SpecificExtraction}
     *
     * @return a Gini Capture SDK {@link GiniCaptureSpecificExtraction}
     */
    @NonNull
    private static GiniCaptureSpecificExtraction map(
            @NonNull final SpecificExtraction source) {
        return new GiniCaptureSpecificExtraction(source.getName(), source.getValue(),
                source.getEntity(),
                BoxMapper.map(source.getBox()),
                ExtractionMapper.mapListToGiniCapture(source.getCandidate()));
    }

    /**
     * Convert a map of {@link GiniCaptureSpecificExtraction}s from the Gini Capture SDK to a map
     * of Gini Bank API lib {@link SpecificExtraction}s.
     *
     * @param sourceMap map of Gini Capture SDK {@link GiniCaptureSpecificExtraction}s
     *
     * @return map of Gini Bank API lib {@link SpecificExtraction}s
     */
    @NonNull
    public static Map<String, SpecificExtraction> mapToApiSdk(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> sourceMap) {
        final Map<String, SpecificExtraction> targetMap = new HashMap<>(sourceMap.size());
        for (final Map.Entry<String, GiniCaptureSpecificExtraction> source : sourceMap.entrySet()) {
            targetMap.put(source.getKey(), map(source.getValue()));
        }
        return targetMap;
    }

    /**
     * Map a {@link GiniCaptureSpecificExtraction} from the Gini Capture SDK to the Gini Bank API lib's
     * {@link SpecificExtraction}.
     *
     * @param source Gini Capture SDK {@link GiniCaptureSpecificExtraction}
     *
     * @return Gini Bank API lib {@link SpecificExtraction}
     */
    @NonNull
    public static SpecificExtraction map(
            @NonNull final GiniCaptureSpecificExtraction source) {
        return new SpecificExtraction(source.getName(), source.getValue(), source.getEntity(),
                BoxMapper.map(source.getBox()),
                ExtractionMapper.mapListToApiSdk(source.getCandidates()));
    }

    private SpecificExtractionMapper() {
    }
}
