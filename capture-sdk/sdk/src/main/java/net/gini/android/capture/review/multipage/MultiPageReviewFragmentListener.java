package net.gini.android.capture.review.multipage;

import android.content.Context;
import android.content.Intent;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.analysis.AnalysisFragmentCompat;
import net.gini.android.capture.camera.CameraFragment;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;

import androidx.annotation.NonNull;

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
     * Called when all pages were uploaded successfully and the user tapped on the "next" button.
     *
     * <p> If you use the Screen API you should start the {@link AnalysisActivity} and set the
     * document as the {@link AnalysisActivity#EXTRA_IN_DOCUMENT} extra.
     *
     * <p> If you use the Component API you should start the {@link AnalysisFragmentCompat}
     * and pass the document when creating it with {@link
     * AnalysisFragmentCompat#createInstance(Document, String)}.
     *
     * @param document contains the reviewed image (can be the original one or a modified image)
     */
    void onProceedToAnalysisScreen(@NonNull GiniCaptureMultiPageDocument document);

    /**
     * Called when the user wants to add a picture of another page. Also called when the user has
     * deleted every page and the document consisted of images taken with the Camera Screen or
     * imported using the Camera Screen.
     *
     * <p> If you host the {@link MultiPageReviewFragment} in its own Activity, then you should
     * simply finish the Activity.
     *
     * <p> If you use one Activity to host all the Gini Capture fragments, then you should display the
     * {@link CameraFragment} again.
     */
    void onReturnToCameraScreenToAddPages();

    /**
     * Called when user deletes the last taken or imported image from {@link MultiPageReviewFragment}
     */
    void onReturnToCameraScreenForFirstPage();

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
