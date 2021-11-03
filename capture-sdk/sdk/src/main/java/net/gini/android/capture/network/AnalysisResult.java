package net.gini.android.capture.network;

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 29.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Used by the {@link GiniCaptureNetworkService} to return analysis results.
 */
public class AnalysisResult extends Result {

    private final Map<String, GiniCaptureSpecificExtraction> extractions;
    private final Map<String, GiniCaptureCompoundExtraction> compoundExtractions;
    private final List<GiniCaptureReturnReason> returnReasons;

    /**
     * Create a new analysis result for a Gini API document id.
     *
     * @param giniApiDocumentId the id of a document in the Gini API
     * @param extractions       the extractions from the Gini API
     */
    public AnalysisResult(@NonNull final String giniApiDocumentId,
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {
        super(giniApiDocumentId);
        this.extractions = extractions;
        this.compoundExtractions = Collections.emptyMap();
        this.returnReasons = Collections.emptyList();
    }

    /**
     * Create a new analysis result for a Gini API document id.
     *
     * @param giniApiDocumentId the id of a document in the Gini API
     * @param extractions       the extractions from the Gini API
     * @param compoundExtractions the compound extractions from the Gini API
     */
    public AnalysisResult(@NonNull final String giniApiDocumentId,
                          @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                          @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions) {
        super(giniApiDocumentId);
        this.extractions = extractions;
        this.compoundExtractions = compoundExtractions;
        this.returnReasons = Collections.emptyList();
    }

    /**
     * Create a new analysis result for a Gini API document id.
     *
     * @param giniApiDocumentId the id of a document in the Gini API
     * @param extractions       the extractions from the Gini API
     * @param compoundExtractions the compound extractions from the Gini API
     */
    public AnalysisResult(@NonNull final String giniApiDocumentId,
                          @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                          @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                          @NonNull final List<GiniCaptureReturnReason> returnReasons) {
        super(giniApiDocumentId);
        this.extractions = extractions;
        this.compoundExtractions = compoundExtractions;
        this.returnReasons = returnReasons;
    }

    /**
     * @return map of extraction labels and specific extractions
     */
    @NonNull
    public Map<String, GiniCaptureSpecificExtraction> getExtractions() {
        return extractions;
    }

    /**
     * @return map of extraction labels and compound extractions
     */
    @NonNull
    public Map<String, GiniCaptureCompoundExtraction> getCompoundExtractions() {
        return compoundExtractions;
    }

    /**
     * @return list of return reasons
     */
    public List<GiniCaptureReturnReason> getReturnReasons() {
        return returnReasons;
    }
}
