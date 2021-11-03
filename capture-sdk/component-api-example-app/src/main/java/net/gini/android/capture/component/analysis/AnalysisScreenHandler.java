package net.gini.android.capture.component.analysis;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.AnalysisFragmentCompat;
import net.gini.android.capture.analysis.AnalysisFragmentInterface;
import net.gini.android.capture.analysis.AnalysisFragmentListener;
import net.gini.android.capture.component.ExtractionsActivity;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.noresults.NoResultsExampleAppCompatActivity;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity.EXTRA_IN_DOCUMENT;
import static net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity.EXTRA_IN_ERROR_MESSAGE;
import static net.gini.android.capture.example.shared.ExampleUtil.getExtractionsBundle;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Contains the logic for the Analysis Screen.
 */
public class AnalysisScreenHandler implements AnalysisFragmentListener {

    private static final Logger LOG = LoggerFactory.getLogger(AnalysisScreenHandler.class);

    private final AppCompatActivity mActivity;
    private AnalysisFragmentInterface mAnalysisFragmentInterface;
    private Document mDocument;
    private String mErrorMessageFromReviewScreen;

    protected AnalysisScreenHandler(final AppCompatActivity activity) {
        mActivity = activity;
    }

    private void showExtractions(final Bundle extractionsBundle) {
        LOG.debug("Show extractions");
        final Intent intent = new Intent(mActivity, ExtractionsActivity.class);
        intent.putExtra(ExtractionsActivity.EXTRA_IN_EXTRACTIONS, extractionsBundle);
        mActivity.startActivity(intent);
        mActivity.setResult(RESULT_OK);
        mActivity.finish();
    }

    private void showNoResultsScreen(final Document document) {
        final Intent intent = getNoResultsActivityIntent(document);
        mActivity.startActivity(intent);
        mActivity.setResult(RESULT_OK);
        mActivity.finish();
    }

    private Intent getNoResultsActivityIntent(final Document document) {
        return NoResultsExampleAppCompatActivity.newInstance(document, mActivity);
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        LOG.error("Gini Capture SDK error: {} - {}", error.getErrorCode(), error.getMessage());
        final Intent result = new Intent();
        result.putExtra(MainActivity.EXTRA_OUT_ERROR, error);
        mActivity.setResult(MainActivity.RESULT_ERROR, result);
        mActivity.finish();
    }

    public Document getDocument() {
        return mDocument;
    }

    protected String getErrorMessageFromReviewScreen() {
        return mErrorMessageFromReviewScreen;
    }

    public void onCreate(final Bundle savedInstanceState) {
        setUpActionBar();
        setTitles();
        readExtras();

        if (savedInstanceState == null) {
            mAnalysisFragmentInterface = createAnalysisFragment();
            showAnalysisFragment();
        } else {
            mAnalysisFragmentInterface = retrieveAnalysisFragment();
        }
    }

    private AnalysisFragmentInterface retrieveAnalysisFragment() {
        mAnalysisFragmentInterface =
                (AnalysisFragmentCompat) mActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.analysis_screen_container);
        return mAnalysisFragmentInterface;
    }

    private void showAnalysisFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.analysis_screen_container, (Fragment) mAnalysisFragmentInterface)
                .commit();
    }

    private AnalysisFragmentInterface createAnalysisFragment() {
        mAnalysisFragmentInterface = AnalysisFragmentCompat.createInstance(getDocument(),
                getErrorMessageFromReviewScreen());
        return mAnalysisFragmentInterface;
    }

    private void readExtras() {
        mDocument = mActivity.getIntent().getParcelableExtra(EXTRA_IN_DOCUMENT);
        mErrorMessageFromReviewScreen = mActivity.getIntent().getStringExtra(
                EXTRA_IN_ERROR_MESSAGE);
    }

    private void setTitles() {
        final ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle("");
        actionBar.setSubtitle(mActivity.getString(R.string.one_moment_please));
    }

    private void setUpActionBar() {
        mActivity.setSupportActionBar(
                (Toolbar) mActivity.findViewById(R.id.toolbar));
    }

    @Override
    public void onExtractionsAvailable(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                                       @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                                       @NonNull final List<GiniCaptureReturnReason> returnReasons) {
        showExtractions(getExtractionsBundle(extractions));
    }

    @Override
    public void onProceedToNoExtractionsScreen(@NonNull final Document document) {
        showNoResultsScreen(document);
    }

    @Override
    public void onDefaultPDFAppAlertDialogCancelled() {
        mActivity.finish();
    }

}
