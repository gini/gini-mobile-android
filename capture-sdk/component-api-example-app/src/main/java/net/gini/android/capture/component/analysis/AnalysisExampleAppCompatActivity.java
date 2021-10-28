package net.gini.android.capture.component.analysis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.AnalysisFragmentCompat;
import net.gini.android.capture.analysis.AnalysisFragmentListener;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.noresults.NoResultsExampleAppCompatActivity;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * AppCompatActivity using the {@link AnalysisScreenHandler} to host the
 * {@link AnalysisFragmentCompat} and to start the {@link ExtractionsActivity} or the
 * {@link NoResultsExampleAppCompatActivity}.
 */
public class AnalysisExampleAppCompatActivity extends AppCompatActivity implements
        AnalysisFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";
    public static final String EXTRA_IN_ERROR_MESSAGE = "EXTRA_IN_ERROR_MESSAGE";
    private AnalysisScreenHandler mAnalysisScreenHandler;

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        mAnalysisScreenHandler.onError(error);
    }

    @Override
    public void onExtractionsAvailable(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                                       @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                                       @NonNull final List<GiniCaptureReturnReason> returnReasons) {
        mAnalysisScreenHandler.onExtractionsAvailable(extractions, compoundExtractions, returnReasons);
    }

    @Override
    public void onProceedToNoExtractionsScreen(@NonNull final Document document) {
        mAnalysisScreenHandler.onProceedToNoExtractionsScreen(document);
    }

    @Override
    public void onDefaultPDFAppAlertDialogCancelled() {
        mAnalysisScreenHandler.onDefaultPDFAppAlertDialogCancelled();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_compat);
        mAnalysisScreenHandler = new AnalysisScreenHandler(this);
        mAnalysisScreenHandler.onCreate(savedInstanceState);
    }

    public static Intent newInstance(final Document document,
            final String errorMessage, final Context context) {
        final Intent intent = new Intent(context, AnalysisExampleAppCompatActivity.class);
        intent.putExtra(EXTRA_IN_DOCUMENT, document);
        intent.putExtra(EXTRA_IN_ERROR_MESSAGE, errorMessage);
        return intent;
    }
}
