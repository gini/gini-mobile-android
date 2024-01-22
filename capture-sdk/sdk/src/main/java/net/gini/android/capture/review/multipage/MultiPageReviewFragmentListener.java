package net.gini.android.capture.review.multipage;

import androidx.annotation.NonNull;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.AnalysisFragment;
import net.gini.android.capture.camera.CameraFragment;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;

/**
 * Created by Alpar Szotyori on 07.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * Interface used by the {@link MultiPageReviewFragment} to dispatch events to the hosting
 * Activity.
 */
public interface MultiPageReviewFragmentListener {

    /**
     * Called when the user deleted all the pages of a document received from another app.
     *
     * <p> At this point you should finish Gini Capture by closing the {@link MultiPageReviewFragment}..
     */
    void onImportedDocumentReviewCancelled();

    /**
     * Called when an error occurred.
     *
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniCaptureError error);
}
