package net.gini.android.capture.analysis;

import androidx.annotation.NonNull;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsFragmentCompat;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Interface used by {@link AnalysisFragmentCompat} to dispatch events to the hosting Activity.
 * </p>
 */
public interface AnalysisFragmentListener {
    /**
     * <p>
     * Called when an error occurred.
     * </p>
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniCaptureError error);

    /**
     * Called when the document has been analyzed and extractions are available.
     *
     * @param extractions a map of the extractions with the extraction labels as keys
     * @param compoundExtractions a map of the compound extractions with the extraction labels as keys
     */
    void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
            @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
            @NonNull final List<GiniCaptureReturnReason> returnReasons);

    /**
     * Called when the document has been analyzed and no extractions were received.
     * <p>
     * You should show the {@link NoResultsFragmentCompat}.
     *
     * @param document contains the reviewed document
     */
    void onProceedToNoExtractionsScreen(@NonNull final Document document);

    /**
     * Called when the default PDF app alert dialog was cancelled. You should close the
     * AnalysisFragment because the user decided not to continue with analysis.
     * <p>
     * This alert dialog is shown for PDFs imported from another app when your app was set as
     * default for opening PDFs.
     */
    void onDefaultPDFAppAlertDialogCancelled();

}
