package net.gini.android.capture.screen;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.onboarding.DefaultPages;
import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureDebug;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.ImportedFileValidationException;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.example.shared.BaseExampleApp;
import net.gini.android.capture.example.shared.RuntimePermissionHandler;
import net.gini.android.capture.help.HelpItem;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLoggerListener;
import net.gini.android.capture.onboarding.DefaultPages;
import net.gini.android.capture.onboarding.OnboardingPage;
import net.gini.android.capture.requirements.GiniCaptureRequirements;
import net.gini.android.capture.requirements.RequirementReport;
import net.gini.android.capture.requirements.RequirementsReport;
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;
import net.gini.android.capture.tracking.Event;
import net.gini.android.capture.tracking.EventTracker;
import net.gini.android.capture.tracking.OnboardingScreenEvent;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter;
import net.gini.android.capture.view.DefaultOnButtonLoadingIndicatorAdapter;
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import static net.gini.android.capture.example.shared.ExampleUtil.isPay5Extraction;

/**
 * Entry point for the screen api example app.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OUT_EXTRACTIONS = "EXTRA_OUT_EXTRACTIONS";

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private static final int REQUEST_SCAN = 1;
    private static final int REQUEST_NO_EXTRACTIONS = 2;

    private Button mButtonStartScanner;
    private boolean mRestoredInstance;
    private RuntimePermissionHandler mRuntimePermissionHandler;
    private TextView mTextGiniCaptureSdkVersion;
    private TextView mTextAppVersion;
    private SwitchMaterial bottomNavBarSwitch;
    private SwitchMaterial animatedOnboardingIllustrationsSwitch;
    private SwitchMaterial customLoadingAnimationSwitch;
    private SwitchMaterial onlyQRCodeSwitch;
    private SwitchMaterial disableCameraPermission;
    private CancellationToken mFileImportCancellationToken;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        addInputHandlers();
        setGiniCaptureSdkDebugging();
        showVersions();
        createRuntimePermissionsHandler();
        mRestoredInstance = savedInstanceState != null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mRestoredInstance) {
            final Intent intent = getIntent();
            if (isIntentActionViewOrSend(intent)) {
                startGiniCaptureSdkForImportedFile(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFileImportCancellationToken != null) {
            mFileImportCancellationToken.cancel();
            mFileImportCancellationToken = null;
        }
    }

    private void createRuntimePermissionsHandler() {
        mRuntimePermissionHandler = RuntimePermissionHandler
                .forActivity(this)
                .withCameraPermissionDeniedMessage(
                        getString(R.string.camera_permission_denied_message))
                .withCameraPermissionRationale(getString(R.string.camera_permission_rationale))
                .withStoragePermissionDeniedMessage(
                        getString(R.string.storage_permission_denied_message))
                .withStoragePermissionRationale(getString(R.string.storage_permission_rationale))
                .withGrantAccessButtonTitle(getString(R.string.grant_access))
                .withCancelButtonTitle(getString(R.string.cancel))
                .build();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (isIntentActionViewOrSend(intent)) {
            startGiniCaptureSdkForImportedFile(intent);
        }
    }

    private void startGiniCaptureSdkForImportedFile(final Intent importedFileIntent) {
        mRuntimePermissionHandler.requestStoragePermission(new RuntimePermissionHandler.Listener() {
            @Override
            public void permissionGranted() {
                doStartGiniCaptureSdkForImportedFile(importedFileIntent);
            }

            @Override
            public void permissionDenied() {
                finish();
            }
        });

    }

    private void doStartGiniCaptureSdkForImportedFile(final Intent importedFileIntent) {
        // Configure the Gini Capture SDK
        configureGiniCapture();
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isMultiPageEnabled()) {
            mFileImportCancellationToken = GiniCapture.getInstance().createIntentForImportedFiles(
                    importedFileIntent, this,
                    new AsyncCallback<Intent, ImportedFileValidationException>() {
                        @Override
                        public void onSuccess(final Intent result) {
                            mFileImportCancellationToken = null;
                            startActivityForResult(result, REQUEST_SCAN);
                        }

                        @Override
                        public void onError(final ImportedFileValidationException exception) {
                            mFileImportCancellationToken = null;
                            handleFileImportError(exception);
                        }

                        @Override
                        public void onCancelled() {
                            mFileImportCancellationToken = null;
                        }
                    });
        } else {
            try {
                final Intent giniCaptureIntent =
                        GiniCapture.createIntentForImportedFile(
                                importedFileIntent,
                                this,
                                null,
                                null);
                startActivityForResult(giniCaptureIntent, REQUEST_SCAN);

            } catch (final ImportedFileValidationException e) {
                e.printStackTrace();
                handleFileImportError(e);
            }
        }
    }

    private void handleFileImportError(final ImportedFileValidationException exception) {
        String message = exception.getMessage();
        if (exception.getValidationError() != null) {
            message = getString(exception.getValidationError().getTextResource());
        }
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface,
                                        final int i) {
                        finish();
                    }
                })
                .show();
    }

    private boolean isIntentActionViewOrSend(@NonNull final Intent intent) {
        final String action = intent.getAction();
        return Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_SEND.equals(action)
                || Intent.ACTION_SEND_MULTIPLE.equals(action);
    }

    @SuppressLint("SetTextI18n")
    private void showVersions() {
        mTextGiniCaptureSdkVersion.setText(
                "Gini Capture SDK v" + net.gini.android.capture.BuildConfig.VERSION_NAME);
        mTextAppVersion.setText("Screen API Example App v" + BuildConfig.VERSION_NAME);
    }

    private void setGiniCaptureSdkDebugging() {
        if (BuildConfig.DEBUG) {
            GiniCaptureDebug.enable();
            configureLogging();
        }
    }

    private void addInputHandlers() {
        mButtonStartScanner.setOnClickListener(v -> {
            if (disableCameraPermission.isChecked()) {
                doStartGiniCaptureSdk();
            } else {
                startGiniCaptureSdk();
            }
        });
    }

    private void startGiniCaptureSdk() {
        mRuntimePermissionHandler.requestCameraPermission(new RuntimePermissionHandler.Listener() {
            @Override
            public void permissionGranted() {
                doStartGiniCaptureSdk();
            }

            @Override
            public void permissionDenied() {
            }
        });
    }

    private void doStartGiniCaptureSdk() {
        // NOTE: on Android 6.0 and later the camera permission is required before checking the requirements
        final RequirementsReport report = GiniCaptureRequirements.checkRequirements(this);
        if (!report.isFulfilled()) {
            // In production apps you should not launch Gini Capture if requirements were not fulfilled
            // We make an exception here to allow running the app on emulators
            showUnfulfilledRequirementsToast(report);
        }

        // Configure the Gini Capture SDK
        configureGiniCapture();

        final Intent intent = new Intent(this, CameraScreenApiActivity.class);

        // Deprecated: custom pages added above when creating the GiniVision instance
        // Uncomment to add an extra page to the Onboarding pages
//        intent.putParcelableArrayListExtra(CameraActivity.EXTRA_IN_ONBOARDING_PAGES, getOnboardingPages());

        // Deprecated: configuration applied above when creating the GiniVision instance
        // Set EXTRA_IN_SHOW_ONBOARDING_AT_FIRST_RUN to false to disable automatically showing the OnboardingActivity the
        // first time the CameraActivity is launched - we highly recommend letting the Gini Capture SDK show the
        // OnboardingActivity at first run
        //intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING_AT_FIRST_RUN, false);

        // Deprecated: configuration applied above when creating the GiniVision instance
        // Set EXTRA_IN_SHOW_ONBOARDING to true, to show the OnboardingActivity when the CameraActivity starts
        //intent.putExtra(CameraActivity.EXTRA_IN_SHOW_ONBOARDING, true);

        // Set EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY to true, to close library on pressing the back
        // button from any Activity in the library
        //intent.putExtra(CameraActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, true);

        // Deprecated: configuration applied above when creating the GiniVision instance
        // Configure the features you would like to use
//        final GiniVisionFeatureConfiguration giniVisionFeatureConfiguration =
//                GiniVisionFeatureConfiguration.buildNewConfiguration()
//                        .setDocumentImportEnabledFileTypes(
//                                DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
//                        .setFileImportEnabled(true)
//                        .setQRCodeScanningEnabled(true)
//                        .build();
//
//        intent.putExtra(CameraActivity.EXTRA_IN_GINI_CAPTURE_FEATURE_CONFIGURATION,
//                giniVisionFeatureConfiguration);

        // Start for result in order to receive the error result, in case something went wrong, or the extractions
        // To receive the extractions add it to the result Intent in ReviewActivity#onAddDataToResult(Intent) or
        // AnalysisActivity#onAddDataToResult(Intent) and retrieve them here in onActivityResult()
        startActivityForResult(intent, REQUEST_SCAN);
    }

    private void configureGiniCapture() {
        final BaseExampleApp app = (BaseExampleApp) getApplication();

        GiniCapture.cleanup(this, "", "", "", "", "");

        app.clearGiniCaptureNetworkInstances();
        final GiniCapture.Builder builder = GiniCapture.newInstance()
                .setGiniCaptureNetworkService(
                        app.getGiniCaptureNetworkService("ScreenAPI")
                )
                .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
                .setFileImportEnabled(true)
                .setQRCodeScanningEnabled(true)
                .setMultiPageEnabled(true);
        builder.setFlashButtonEnabled(true);
        builder.setEventTracker(new GiniCaptureEventTracker());
        builder.setCustomErrorLoggerListener(new CustomErrorLoggerListener());
        builder.setReviewBottomBarNavigationAdapter(new DefaultReviewNavigationBarBottomAdapter());
        builder.setLoadingIndicatorAdapter(new DefaultLoadingIndicatorAdapter());
        // Uncomment to disable sending errors to Gini
//        builder.setGiniErrorLoggerIsOn(false);

        final List<HelpItem.Custom> customHelpItems = new ArrayList<>();
        customHelpItems.add(new HelpItem.Custom(R.string.custom_help_screen_title,
                new Intent(this, CustomHelpActivity.class)));
        builder.setCustomHelpItems(customHelpItems);

        // Uncomment to turn off the camera flash by default
//        builder.setFlashOnByDefault(false);
        // Uncomment to disable back buttons (except in the review and analysis screens)
//        builder.setBackButtonsEnabled(false);
        // Uncomment to add an extra page to the Onboarding pages
//                builder.setCustomOnboardingPages(getOnboardingPages());
        // Uncomment to disable automatically showing the OnboardingActivity the
        // first time the CameraActivity is launched - we highly recommend letting the
        // Gini Capture SDK show the OnboardingActivity at first run
//                builder.setShouldShowOnboardingAtFirstRun(false);
        // Uncomment to show the OnboardingActivity every time the CameraActivity starts
//                builder.setShouldShowOnboarding(true);
        // Uncomment to remove the Supported Formats help screen
//                builder.setSupportedFormatsHelpScreenEnabled(false);

        builder.setBottomNavigationBarEnabled(bottomNavBarSwitch.isChecked());
        if (animatedOnboardingIllustrationsSwitch.isChecked()) {
            builder.setOnboardingAlignCornersIllustrationAdapter(new CustomOnboardingIllustrationAdapter(getResources().getIdentifier("floating_document", "raw", this.getPackageName())));
            builder.setOnboardingLightingIllustrationAdapter(new CustomOnboardingIllustrationAdapter(getResources().getIdentifier("lighting", "raw", this.getPackageName())));
            builder.setOnboardingMultiPageIllustrationAdapter(new CustomOnboardingIllustrationAdapter(getResources().getIdentifier("multipage", "raw", this.getPackageName())));
            builder.setOnboardingQRCodeIllustrationAdapter(new CustomOnboardingIllustrationAdapter(getResources().getIdentifier("scan_qr_code", "raw", this.getPackageName())));
        }

        if (customLoadingAnimationSwitch.isChecked()) {
            builder.setLoadingIndicatorAdapter(new CustomLottiLoadingIndicatorAdapter(getResources().getIdentifier("custom_loading", "raw", this.getPackageName())));
        }

        builder.setOnlyQRCodeScanning(onlyQRCodeSwitch.isChecked());

        builder.build();
    }

    private void showUnfulfilledRequirementsToast(final RequirementsReport report) {
        final StringBuilder stringBuilder = new StringBuilder();
        final List<RequirementReport> requirementReports = report.getRequirementReports();
        for (int i = 0; i < requirementReports.size(); i++) {
            final RequirementReport requirementReport = requirementReports.get(i);
            if (!requirementReport.isFulfilled()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("\n");
                }
                stringBuilder.append(requirementReport.getRequirementId());
                if (!requirementReport.getDetails().isEmpty()) {
                    stringBuilder.append(": ");
                    stringBuilder.append(requirementReport.getDetails());
                }
            }
        }
        Toast.makeText(this, "Requirements not fulfilled:\n" + stringBuilder,
                Toast.LENGTH_LONG).show();
    }

    private void bindViews() {
        mButtonStartScanner = (Button) findViewById(R.id.button_start_scanner);
        mTextGiniCaptureSdkVersion = (TextView) findViewById(R.id.text_gini_capture_version);
        mTextAppVersion = (TextView) findViewById(R.id.text_app_version);
        bottomNavBarSwitch = findViewById(R.id.bottom_navbar_switch);
        animatedOnboardingIllustrationsSwitch = findViewById(R.id.animated_onboarding_illustrations_switch);
        customLoadingAnimationSwitch = findViewById(R.id.custom_loading_indicator_switch);
        onlyQRCodeSwitch = findViewById(R.id.gc_only_qr_code_scanning);
        disableCameraPermission = findViewById(R.id.gc_disable_camera_permision);
    }

    private ArrayList<OnboardingPage> getOnboardingPages(final boolean isMultiPageEnabled, final boolean isQRCodeScanningEnabled) {
        // Adding a custom page to the default pages
        final ArrayList<OnboardingPage> pages = DefaultPages.asArrayList(isMultiPageEnabled, isQRCodeScanningEnabled);
        pages.add(new OnboardingPage(R.string.additional_onboarding_page_title, R.string.additional_onboarding_page_message, null));
        return pages;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN) {
            if (data == null) {
                if (isIntentActionViewOrSend(getIntent())) {
                    finish();
                }

                if (resultCode == CameraActivity.RESULT_ENTER_MANUALLY) {
                    handleEnterManuallyAction();
                }
                return;
            }
            switch (resultCode) {
                case CameraActivity.RESULT_ENTER_MANUALLY:
                    handleEnterManuallyAction();
                    break;
                case RESULT_CANCELED:
                    break;
                case RESULT_OK:
                    // Retrieve the extractions
                    Bundle extractionsBundle = data.getBundleExtra(
                            CameraActivity.EXTRA_OUT_EXTRACTIONS);
                    if (extractionsBundle == null) {
                        extractionsBundle = data.getBundleExtra(MainActivity.EXTRA_OUT_EXTRACTIONS);
                    }
                    if (extractionsBundle == null) {
                        if (isIntentActionViewOrSend(getIntent())) {
                            finish();
                        }
                        return;
                    }
                    final Bundle compoundExtractionsBundle = data.getBundleExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS);
                    if (pay5ExtractionsAvailable(extractionsBundle)
                            || epsPaymentAvailable(extractionsBundle)
                            || compoundExtractionsBundle != null) {
                        startExtractionsActivity(extractionsBundle, compoundExtractionsBundle);
                    } else {
                        // Show a special screen, if no Pay5 extractions were found to give
                        // the user some hints and tips
                        // for using the Gini Capture SDK
                        startNoExtractionsActivity();
                    }
                    break;
                case CameraActivity.RESULT_ERROR:
                    // Something went wrong, retrieve and show the error
                    final GiniCaptureError error = data.getParcelableExtra(
                            CameraActivity.EXTRA_OUT_ERROR);
                    if (error != null) {
                        Toast.makeText(this, "Error: "
                                        + error.getErrorCode() + " - "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    break;
            }
            if (isIntentActionViewOrSend(getIntent())) {
                finish();
            }
        } else if (requestCode == REQUEST_NO_EXTRACTIONS) {
            // The NoExtractionsActivity has a button for taking another picture which causes the activity to finish
            // and return the result code seen below
            if (resultCode == NoExtractionsActivity.RESULT_START_GINI_CAPTURE) {
                startGiniCaptureSdk();
            }
        }
    }

    private boolean pay5ExtractionsAvailable(final Bundle extractionsBundle) {
        for (final String key : extractionsBundle.keySet()) {
            if (isPay5Extraction(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean epsPaymentAvailable(final Bundle extractionsBundle) {
        for (final String key : extractionsBundle.keySet()) {
            if (key.equals("epsPaymentQRCodeUrl")) {
                return true;
            }
        }
        return false;
    }

    private void startNoExtractionsActivity() {
        final Intent intent = new Intent(this, NoExtractionsActivity.class);
        startActivityForResult(intent, REQUEST_NO_EXTRACTIONS);
    }

    private void startExtractionsActivity(@NonNull final Bundle extractionsBundle, @Nullable final Bundle compoundExtractionsBundle) {
        final Intent intent = new Intent(this, ExtractionsActivity.class);
        intent.putExtra(ExtractionsActivity.EXTRA_IN_EXTRACTIONS, extractionsBundle);
        intent.putExtra(ExtractionsActivity.EXTRA_IN_COMPOUND_EXTRACTIONS, compoundExtractionsBundle);
        startActivity(intent);
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

    private void handleEnterManuallyAction() {
        Toast.makeText(this, "Scan exited for manual enter mode", Toast.LENGTH_SHORT).show();
    }

    private static class GiniCaptureEventTracker implements EventTracker {

        @Override
        public void onOnboardingScreenEvent(final Event<OnboardingScreenEvent> event) {
            switch (event.getType()) {
                case START:
                    LOG.info("Onboarding started");
                    break;
                case FINISH:
                    LOG.info("Onboarding finished");
                    break;
            }
        }

        @Override
        public void onCameraScreenEvent(final Event<CameraScreenEvent> event) {
            switch (event.getType()) {
                case TAKE_PICTURE:
                    LOG.info("Take picture");
                    break;
                case HELP:
                    LOG.info("Show help");
                    break;
                case EXIT:
                    LOG.info("Exit");
                    break;
            }
        }

        @Override
        public void onReviewScreenEvent(final Event<ReviewScreenEvent> event) {
            switch (event.getType()) {
                case NEXT:
                    LOG.info("Go next to analyse");
                    break;
                case BACK:
                    LOG.info("Go back to the camera");
                    break;
                case UPLOAD_ERROR:
                    final Throwable error = (Throwable) event.getDetails().get(ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT);
                    LOG.info("Upload failed:\nmessage: {}\nerror:",
                            event.getDetails().get(ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE),
                            error);
                    break;
            }
        }

        @Override
        public void onAnalysisScreenEvent(final Event<AnalysisScreenEvent> event) {
            switch (event.getType()) {
                case ERROR:
                    final Throwable error = (Throwable) event.getDetails().get(AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT);
                    LOG.info("Analysis failed:\nmessage: {}\nerror:",
                            event.getDetails().get(AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE),
                            error);
                    break;
                case RETRY:
                    LOG.info("Retry analysis");
                    break;
                case CANCEL:
                    LOG.info("Analysis cancelled");
                    break;
            }
        }
    }

    private static class CustomErrorLoggerListener implements ErrorLoggerListener {

        @Override
        public void handleErrorLog(@NonNull ErrorLog errorLog) {
            LOG.error("Custom error logger: {}", errorLog.toString());
        }
    }
}

