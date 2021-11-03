package net.gini.android.capture.component.camera;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureCoordinator;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImportedFileValidationException;
import net.gini.android.capture.camera.CameraFragmentCompat;
import net.gini.android.capture.camera.CameraFragmentInterface;
import net.gini.android.capture.camera.CameraFragmentListener;
import net.gini.android.capture.component.ExtractionsActivity;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity;
import net.gini.android.capture.component.review.ReviewExampleAppCompatActivity;
import net.gini.android.capture.component.review.multipage.MultiPageReviewExampleActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.example.shared.BaseExampleApp;
import net.gini.android.capture.example.shared.SingleDocumentAnalyzer;
import net.gini.android.capture.help.HelpActivity;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.onboarding.OnboardingFragmentCompat;
import net.gini.android.capture.onboarding.OnboardingFragmentListener;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.util.IntentHelper;
import net.gini.android.capture.util.UriHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import static android.app.Activity.RESULT_OK;
import static net.gini.android.capture.example.shared.ExampleUtil.getExtractionsBundle;
import static net.gini.android.capture.example.shared.ExampleUtil.isIntentActionViewOrSend;

/**
 * Contains the logic for the Camera Screen.
 */
public class CameraScreenHandler implements CameraFragmentListener,
        OnboardingFragmentListener {

    // Set to true to allow execution of the custom code check
    private static final boolean DO_CUSTOM_DOCUMENT_CHECK = false;
    private static final Logger LOG = LoggerFactory.getLogger(CameraScreenHandler.class);
    private static final int REVIEW_REQUEST = 1;
    private static final int MULTI_PAGE_REVIEW_REQUEST = 2;
    private static final int ANALYSIS_REQUEST = 3;
    private final AppCompatActivity mActivity;
    private CameraFragmentInterface mCameraFragmentInterface;
    private GiniCaptureCoordinator mGiniCaptureCoordinator;
    private Menu mMenu;
    private SingleDocumentAnalyzer mSingleDocumentAnalyzer;
    private CancellationToken mFileImportCancellationToken;

    private final ActivityResultLauncher<Intent> startReview;
    private final ActivityResultLauncher<Intent> startMultiPageReview;
    private final ActivityResultLauncher<Intent> startAnalysis;

    protected CameraScreenHandler(final AppCompatActivity activity,
                                  ActivityResultLauncher<Intent> startReview,
                                  ActivityResultLauncher<Intent> startMultiPageReview,
                                  ActivityResultLauncher<Intent> startAnalysis) {
        mActivity = activity;
        this.startReview = startReview;
        this.startMultiPageReview = startMultiPageReview;
        this.startAnalysis = startAnalysis;
    }

    @Override
    public void onCloseOnboarding() {
        removeOnboarding();
    }

    private void removeOnboarding() {
        LOG.debug("Remove the Onboarding Screen");
        if (mMenu != null) {
            mMenu.setGroupVisible(R.id.group, true);
        }
        mCameraFragmentInterface.showInterface();
        setTitlesForCamera();
        removeOnboardingFragment();
    }

    private void removeOnboardingFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentById(
                R.id.onboarding_container);
        if (fragment != null) {
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    private void setTitlesForCamera() {
        final ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(R.string.camera_screen_title);
        actionBar.setSubtitle(mActivity.getString(R.string.camera_screen_subtitle));
    }

    @Override
    public void onDocumentAvailable(@NonNull final Document document) {
        LOG.debug("Document available {}", document);
        // Cancel analysis to make sure, that the document analysis will start in
        // onShouldAnalyzeDocument()
        getSingleDocumentAnalyzer().cancelAnalysis();
        if (document.isReviewable()) {
            launchReviewScreen(document);
        } else {
            launchAnalysisScreen(document);
        }
    }

    private SingleDocumentAnalyzer getSingleDocumentAnalyzer() {
        if (mSingleDocumentAnalyzer == null) {
            mSingleDocumentAnalyzer =
                    ((BaseExampleApp) mActivity.getApplication()).getSingleDocumentAnalyzer();
        }
        return mSingleDocumentAnalyzer;
    }

    @Override
    public void onCheckImportedDocument(@NonNull final Document document,
            @NonNull final DocumentCheckResultCallback callback) {
        // We can apply custom checks here to an imported document and notify the Gini Capture SDK
        // about the result

        // As an example we allow only documents smaller than 5MB
        if (DO_CUSTOM_DOCUMENT_CHECK) {
            // Use the Intent with which the document was imported to access its contents
            // (document.getData() may be null)
            final Intent intent = document.getIntent();
            if (intent == null) {
                callback.documentRejected(mActivity.getString(R.string.gc_document_import_error));
                return;
            }
            final Uri uri = IntentHelper.getUri(intent);
            if (uri == null) {
                callback.documentRejected(mActivity.getString(R.string.gc_document_import_error));
                return;
            }
            // IMPORTANT: always call one of the callback methods
            if (hasLessThan5MB(callback, uri)) {
                callback.documentAccepted();
            } else {
                callback.documentRejected(mActivity.getString(R.string.document_size_too_large));
            }
        } else {
            // IMPORTANT: always call one of the callback methods
            callback.documentAccepted();
        }
    }

    private boolean hasLessThan5MB(@NonNull final DocumentCheckResultCallback callback,
            final Uri uri) {
        final int fileSize = UriHelper.getFileSizeFromUri(uri, mActivity);
        return fileSize <= 5 * 1024 * 1024;
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        LOG.error("Gini Capture SDK error: {} - {}", error.getErrorCode(), error.getMessage());
        final Intent result = new Intent();
        result.putExtra(MainActivity.EXTRA_OUT_ERROR, error);
        mActivity.setResult(MainActivity.RESULT_ERROR, result);
        mActivity.finish();
    }

    private void launchReviewScreen(final Document document) {
        startReview.launch(getReviewActivityIntent(document));
    }

    private Intent getReviewActivityIntent(final Document document) {
        return ReviewExampleAppCompatActivity.newInstance(document, mActivity);
    }

    private void launchAnalysisScreen(final Document document) {
        startAnalysis.launch(getAnalysisActivityIntent(document));
    }

    private Intent getAnalysisActivityIntent(final Document document) {
        return AnalysisExampleAppCompatActivity.newInstance(document, null, mActivity);
    }

    protected Activity getActivity() {
        return mActivity;
    }

    public boolean onBackPressed() {
        if (isOnboardingVisible()) {
            removeOnboarding();
            return true;
        }
        if (mFileImportCancellationToken != null) {
            mFileImportCancellationToken.cancel();
            mFileImportCancellationToken = null;
        }
        return false;
    }

    private boolean isOnboardingVisible() {
        return mActivity.getSupportFragmentManager().findFragmentById(
                R.id.onboarding_container) != null;
    }

    public void onCreate(final Bundle savedInstanceState) {
        setUpActionBar();
        setTitlesForCamera();

        configureLogging();
        setupGiniCaptureCoordinator(mActivity);

        // Deprecated: configuration applied in MainActivity#initGiniVision()
        // Configure the features you would like to use
//        mGiniVisionFeatureConfiguration =
//                GiniVisionFeatureConfiguration.buildNewConfiguration()
//                        .setDocumentImportEnabledFileTypes(
//                                DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
//                        .setFileImportEnabled(true)
//                        .setQRCodeScanningEnabled(true)
//                        .build();

        if (savedInstanceState == null) {
            final Intent intent = mActivity.getIntent();
            if (isIntentActionViewOrSend(intent)) {
                startGiniCaptureSdkForImportedFile(intent);
            } else {
                showCamera();
            }
        } else {
            mCameraFragmentInterface = retrieveCameraFragment();
        }
    }

    private void setUpActionBar() {
        mActivity.setSupportActionBar(
                (Toolbar) mActivity.findViewById(R.id.toolbar));
    }

    private void showCamera() {
        LOG.debug("Show the Camera Screen");
        mCameraFragmentInterface = createCameraFragment();
        showCameraFragment();
        // Delay notifying the coordinator to allow the camera fragment view to be created
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mGiniCaptureCoordinator.onCameraStarted();
            }
        });
    }

    private CameraFragmentInterface createCameraFragment() {
        mCameraFragmentInterface = CameraFragmentCompat.createInstance();
        return mCameraFragmentInterface;
    }

    private void showCameraFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.camera_container, (Fragment) mCameraFragmentInterface)
                .commit();
    }

    private CameraFragmentInterface retrieveCameraFragment() {
        mCameraFragmentInterface =
                (CameraFragmentCompat) mActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.camera_container);
        return mCameraFragmentInterface;
    }

    private void configureLogging() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();

        final PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
        layoutEncoder.setContext(lc);
        layoutEncoder.setPattern("%-5level %file:%line [%thread] - %msg%n");
        layoutEncoder.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(layoutEncoder);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(logcatAppender);
    }

    private void setupGiniCaptureCoordinator(final Activity activity) {
        mGiniCaptureCoordinator = GiniCaptureCoordinator.createInstance(activity);
        mGiniCaptureCoordinator.setListener(new GiniCaptureCoordinator.Listener() {
            @Override
            public void onShowOnboarding() {
                showOnboarding();
            }
        });
    }

    private void showOnboarding() {
        LOG.debug("Show the Onboarding Screen");
        if (mMenu != null) {
            mMenu.setGroupVisible(R.id.group, false);
        }
        mCameraFragmentInterface.hideInterface();
        setTitlesForOnboarding();
        showOnboardingFragment();
    }

    private void showOnboardingFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.onboarding_container, new OnboardingFragmentCompat())
                .commit();
    }

    private void setTitlesForOnboarding() {
        final ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle("");
        actionBar.setSubtitle("");
    }

    private void startGiniCaptureSdkForImportedFile(@NonNull final Intent importedFileIntent) {
        getSingleDocumentAnalyzer().cancelAnalysis();
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isMultiPageEnabled()) {
            mFileImportCancellationToken = GiniCapture.getInstance().createDocumentForImportedFiles(
                    importedFileIntent, mActivity,
                    new AsyncCallback<Document, ImportedFileValidationException>() {
                        @Override
                        public void onSuccess(@NonNull final Document result) {
                            if (result.isReviewable()) {
                                launchMultiPageReviewScreen();
                            } else {
                                launchAnalysisScreen(result);
                            }
                            mActivity.finish();
                        }

                        @Override
                        public void onError(
                                @NonNull final ImportedFileValidationException exception) {
                            handleFileImportError(exception);
                        }

                        @Override
                        public void onCancelled() {

                        }
                    });
        } else {
            try {
                final Document document = GiniCapture.createDocumentForImportedFile(
                        importedFileIntent,
                        mActivity);
                if (document.isReviewable()) {
                    launchReviewScreen(document);
                } else {
                    launchAnalysisScreen(document);
                }
                mActivity.finish();

            } catch (final ImportedFileValidationException e) {
                e.printStackTrace();
                handleFileImportError(e);
            }
        }
    }

    private void handleFileImportError(final ImportedFileValidationException exception) {
        String message = exception.getMessage();
        if (exception.getValidationError() != null) {
            message = mActivity.getString(exception.getValidationError().getTextResource());
        }
        new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int i) {
                        mActivity.finish();
                    }
                })
                .show();
    }

    private void launchMultiPageReviewScreen() {
        startMultiPageReview.launch(MultiPageReviewExampleActivity.newInstance(mActivity));
    }

    public boolean onCreateOptionsMenu(final Menu menu) {
        mMenu = menu;
        mActivity.getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    public void onNewIntent(final Intent intent) {
        if (isIntentActionViewOrSend(intent)) {
            startGiniCaptureSdkForImportedFile(intent);
        }
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tips:
                showOnboarding();
                return true;
            case R.id.help:
                showHelp();
                return true;
        }
        return false;
    }

    private void showHelp() {
        final Intent intent = new Intent(mActivity, HelpActivity.class);
        mActivity.startActivity(intent);
    }

    @Override
    public void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {
        final Intent intent = new Intent(mActivity, ExtractionsActivity.class);
        intent.putExtra(ExtractionsActivity.EXTRA_IN_EXTRACTIONS,
                getExtractionsBundle(extractions));
        mActivity.startActivity(intent);
        mActivity.setResult(Activity.RESULT_OK);
        mActivity.finish();
    }

    @Override
    public void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        // Only compat version available (which uses the support library)
        launchMultiPageReviewScreen();
    }
}
