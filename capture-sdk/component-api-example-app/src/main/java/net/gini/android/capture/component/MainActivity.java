package net.gini.android.capture.component;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.core.api.GiniApiType;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureDebug;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.component.camera.CameraExampleAppCompatActivity;
import net.gini.android.capture.example.shared.BaseExampleApp;
import net.gini.android.capture.example.shared.RuntimePermissionHandler;
import net.gini.android.capture.onboarding.DefaultPagesPhone;
import net.gini.android.capture.onboarding.OnboardingPage;
import net.gini.android.capture.requirements.GiniCaptureRequirements;
import net.gini.android.capture.requirements.RequirementReport;
import net.gini.android.capture.requirements.RequirementsReport;

import java.util.ArrayList;
import java.util.List;

import static net.gini.android.capture.example.shared.ExampleUtil.isIntentActionViewOrSend;

/**
 * Entry point for the component api example app.
 */
public class MainActivity extends AppCompatActivity {

    private Button mButtonStartGiniCapture;
    private boolean mRestoredInstance;
    private RuntimePermissionHandler mRuntimePermissionHandler;
    private TextView mTextAppVersion;
    private TextView mTextGiniCaptureSdkVersion;

    public static final int RESULT_ERROR = RESULT_FIRST_USER + 1;
    public static final String EXTRA_OUT_ERROR = "EXTRA_OUT_ERROR";

    private final ActivityResultLauncher<Intent> mStartGVL = registerForActivityResult(
            new CaptureComponentContract(),
            new ActivityResultCallback<CaptureComponentContract.Result>() {
                @Override
                public void onActivityResult(CaptureComponentContract.Result result) {
                    if (result.getError() != null) {
                        Toast.makeText(MainActivity.this, "Error: "
                                        + result.getError().getErrorCode() + " - "
                                        + result.getError().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

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
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (isIntentActionViewOrSend(intent)) {
            startGiniCaptureSdkForImportedFile(intent);
        }
    }

    private void initGiniCapture() {
        final BaseExampleApp app = (BaseExampleApp) getApplication();
        GiniCapture.cleanup(this);
        app.clearGiniCaptureNetworkInstances();
        final GiniCapture.Builder builder = GiniCapture.newInstance()
                .setGiniCaptureNetworkService(
                        app.getGiniCaptureNetworkService("ComponentAPI")
                ).setGiniCaptureNetworkApi(app.getGiniCaptureNetworkApi());
        builder.setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
                .setFileImportEnabled(true)
                .setQRCodeScanningEnabled(true)
                .setMultiPageEnabled(true);
        builder.setFlashButtonEnabled(true);
        // Uncomment to turn off the camera flash by default
//        builder.setFlashOnByDefault(false);
        // Uncomment to add an extra page to the Onboarding pages
//        builder.setCustomOnboardingPages(getOnboardingPages());
        // Uncomment to remove the Supported Formats help screen
//        builder.setSupportedFormatsHelpScreenEnabled(false);
        builder.build();
    }

    private ArrayList<OnboardingPage> getOnboardingPages() {
        // Adding a custom page to the default pages
        final ArrayList<OnboardingPage> pages = DefaultPagesPhone.asArrayList();
        pages.add(new OnboardingPage(R.string.additional_onboarding_page,
                R.drawable.additional_onboarding_illustration));
        return pages;
    }

    private void startGiniCaptureSdkForImportedFile(final Intent importedFileIntent) {
        initGiniCapture();
        startGiniCaptureForImportedFile(importedFileIntent);
    }

    private void startGiniCaptureForImportedFile(@Nullable final Intent importedFileIntent) {
        mRuntimePermissionHandler.requestStoragePermission(
                new RuntimePermissionHandler.Listener() {
                    @Override
                    public void permissionGranted() {
                        final Intent intent = new Intent(importedFileIntent);
                        intent.setClass(MainActivity.this, CameraExampleAppCompatActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void permissionDenied() {
                        finish();
                    }
                });
    }

    private void addInputHandlers() {
        mButtonStartGiniCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startGiniCapture();
            }
        });
    }

    private void startGiniCapture() {
        initGiniCapture();
        mRuntimePermissionHandler.requestCameraPermission(
                new RuntimePermissionHandler.Listener() {
                    @Override
                    public void permissionGranted() {
                        // NOTE: on Android 6.0 and later the camera permission is required before checking the requirements
                        final RequirementsReport report = GiniCaptureRequirements.checkRequirements(
                                MainActivity.this);
                        if (!report.isFulfilled()) {
                            // In production apps you should not launch Gini Capture if requirements were not fulfilled
                            // We make an exception here to allow running the app on emulators
                            showUnfulfilledRequirementsToast(report);
                        }
                        mStartGVL.launch(CameraExampleAppCompatActivity.newInstance(MainActivity.this));
                    }

                    @Override
                    public void permissionDenied() {

                    }
                });
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
        Toast.makeText(this, getString(R.string.unfulfilled_requirements, stringBuilder),
                Toast.LENGTH_LONG).show();
    }

    private void bindViews() {
        mButtonStartGiniCapture = findViewById(R.id.button_start_gini_capture);
        mTextGiniCaptureSdkVersion = findViewById(R.id.text_gini_capture_version);
        mTextAppVersion = findViewById(R.id.text_app_version);
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

    private void setGiniCaptureSdkDebugging() {
        if (BuildConfig.DEBUG) {
            GiniCaptureDebug.enable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetTextI18n")
    private void showVersions() {
        mTextGiniCaptureSdkVersion.setText(
                "Gini Capture SDK v" + net.gini.android.capture.BuildConfig.VERSION_NAME);
        mTextAppVersion.setText("v" + BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
