package net.gini.android.capture.analysis;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class AnalysisFragmentHostActivity extends
        AnalysisFragmentHostActivityNotListener implements AnalysisFragmentListener {

    @Override
    public void onError(@NonNull final GiniCaptureError error) {

    }

    @Override
    public void onExtractionsAvailable(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                                       @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                                       @NonNull final List<GiniCaptureReturnReason> returnReasons) {

    }

    @Override
    public void onProceedToNoExtractionsScreen(@NonNull final Document document) {

    }

    @Override
    public void onDefaultPDFAppAlertDialogCancelled() {

    }
}
