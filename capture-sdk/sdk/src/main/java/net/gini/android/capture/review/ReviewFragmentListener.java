package net.gini.android.capture.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.AnalysisActivity;

/**
 * Interface used by {@link ReviewFragmentCompat} to dispatch
 * events to the hosting Activity.
 */
public interface ReviewFragmentListener {

    /**
     * Called when an error occurred.
     *
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniCaptureError error);

    /**
     * Called when the user tapped on the Next button and one of the following conditions apply:
     * <ul> <li>Analysis is in progress <li>Analysis completed with an error <li>The image was
     * rotated
     *
     * <p> You should start your Activity extending {@link AnalysisActivity} and set the document as
     * the {@link AnalysisActivity#EXTRA_IN_DOCUMENT} extra.
     *
     * @param document     contains the reviewed image (can be the original one or a modified
     *                     image)
     * @param errorMessage an optional error message to be passed to the Analysis Screen
     */
    void onProceedToAnalysisScreen(@NonNull Document document, @Nullable String errorMessage);
}
