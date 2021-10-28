package net.gini.android.capture.camera;

import androidx.annotation.NonNull;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.Map;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class CameraFragmentHostActivity extends CameraFragmentHostActivityNotListener implements
        CameraFragmentListener {

    private boolean hasDocument = false;

    public boolean hasDocument() {
        return hasDocument;
    }

    @Override
    public void onDocumentAvailable(@NonNull final Document document) {
        hasDocument = true;
    }

    @Override
    public void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {

    }

    @Override
    public void onCheckImportedDocument(@NonNull final Document document,
            @NonNull final DocumentCheckResultCallback callback) {

    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {

    }

    @Override
    public void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {

    }
}
