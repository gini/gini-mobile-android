package net.gini.android.capture.screen.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.BuildConfig
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureDebug
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.help.HelpItem.Custom
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.onboarding.DefaultPages.Companion.asArrayList
import net.gini.android.capture.onboarding.OnboardingPage
import net.gini.android.capture.requirements.GiniCaptureRequirements
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.ScreenApiExampleApp
import net.gini.android.capture.screen.core.ExampleUtil
import net.gini.android.capture.screen.core.RuntimePermissionHandler
import net.gini.android.capture.screen.databinding.ActivityMainBinding
import net.gini.android.capture.screen.ui.data.Configuration
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.util.CancellationToken
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import org.slf4j.LoggerFactory
import javax.inject.Inject


/**
 * Entry point for the screen api example app.
 */

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var binding: ActivityMainBinding

    private lateinit var configurationViewModel: ConfigurationViewModel


    @Inject
    lateinit var giniCaptureDefaultNetworkService: GiniCaptureDefaultNetworkService
    private var mRestoredInstance = false
    private lateinit var mRuntimePermissionHandler: RuntimePermissionHandler
    private var mFileImportCancellationToken: CancellationToken? = null
    private lateinit var configuration: Configuration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurationViewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                configurationViewModel.configuration.collect {
                    configuration = it
                }
            }
        }

        addInputHandlers()
        setGiniCaptureSdkDebugging()
        showVersions()
        createRuntimePermissionsHandler()
        mRestoredInstance = savedInstanceState != null
        /*isQRenabledSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (!isChecked) {
                onlyQRCodeSwitch!!.isChecked = false
            }
        }*/
    }

    override fun onStart() {
        super.onStart()
        if (!mRestoredInstance) {
            val intent = intent
            if (isIntentActionViewOrSend(intent)) {
                startGiniCaptureSdkForImportedFile(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mFileImportCancellationToken != null) {
            mFileImportCancellationToken!!.cancel()
            mFileImportCancellationToken = null
        }
    }

    private fun createRuntimePermissionsHandler() {
        mRuntimePermissionHandler = RuntimePermissionHandler
            .forActivity(this)
            .withCameraPermissionDeniedMessage(
                getString(R.string.camera_permission_denied_message)
            )
            .withCameraPermissionRationale(getString(R.string.camera_permission_rationale))
            .withStoragePermissionDeniedMessage(
                getString(R.string.storage_permission_denied_message)
            )
            .withStoragePermissionRationale(getString(R.string.storage_permission_rationale))
            .withGrantAccessButtonTitle(getString(R.string.grant_access))
            .withCancelButtonTitle(getString(R.string.cancel))
            .build()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isIntentActionViewOrSend(intent)) {
            startGiniCaptureSdkForImportedFile(intent)
        }
    }

    private fun startGiniCaptureSdkForImportedFile(importedFileIntent: Intent) {
        mRuntimePermissionHandler.requestStoragePermission(object :
            RuntimePermissionHandler.Listener {
            override fun permissionGranted() {
                doStartGiniCaptureSdkForImportedFile(importedFileIntent)
            }

            override fun permissionDenied() {
                finish()
            }
        })
    }

    private fun doStartGiniCaptureSdkForImportedFile(importedFileIntent: Intent) {
        // Configure the Gini Capture SDK
        configureGiniCapture()
        mFileImportCancellationToken = GiniCapture.getInstance().createIntentForImportedFiles(
            importedFileIntent, this,
            object : AsyncCallback<Intent, ImportedFileValidationException> {
                override fun onSuccess(result: Intent) {
                    mFileImportCancellationToken = null
                    startActivityForResult(result, REQUEST_SCAN)
                }

                override fun onError(exception: ImportedFileValidationException) {
                    mFileImportCancellationToken = null
                    // For Alpar -> is it okay? it is because we are calling Java from Kotlin!
                    handleFileImportError(exception)
                }

                override fun onCancelled() {
                    mFileImportCancellationToken = null
                }
            })
    }

    private fun handleFileImportError(exception: ImportedFileValidationException) {
        var message = exception.message
        if (exception.validationError != null) {
            message = getString(exception.validationError!!.textResource)
        }
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialogInterface, i -> finish() }
            .show()
    }

    private fun isIntentActionViewOrSend(intent: Intent): Boolean {
        val action = intent.action
        return Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action
    }

    @SuppressLint("SetTextI18n")
    private fun showVersions() {
        binding.textGiniCaptureVersion.text =
            getString(R.string.gini_capture_sdk_version) + BuildConfig.VERSION_NAME
    }

    private fun setGiniCaptureSdkDebugging() {
        if (BuildConfig.DEBUG) {
            GiniCaptureDebug.enable()
            configureLogging()
        }
    }

    private fun addInputHandlers() {
        binding.buttonStartScanner.setOnClickListener { v: View? ->
            //TODO: set from configuration object and delete the line below
            startGiniCaptureSdk()
            /*if (disableCameraPermission.isChecked) {
                doStartGiniCaptureSdk()
            } else {
                startGiniCaptureSdk()
            }*/
        }

        binding.textGiniCaptureVersion.setOnClickListener {
            startActivityForResult(
                Intent(
                    this,
                    ConfigurationActivity::class.java
                ).putExtra(CONFIGURATION_BUNDLE, configuration), REQUEST_CONFIGURATION
            )
        }

    }

    private fun startGiniCaptureSdk() {
        mRuntimePermissionHandler.requestCameraPermission(object :
            RuntimePermissionHandler.Listener {
            override fun permissionGranted() {
                doStartGiniCaptureSdk()
            }

            override fun permissionDenied() {}
        })


    }

    private fun doStartGiniCaptureSdk() {
        // NOTE: on Android 6.0 and later the camera permission is required before checking the requirements
        val report = GiniCaptureRequirements.checkRequirements(this)
        if (!report.isFulfilled) {
            // In production apps you should not launch Gini Capture if requirements were not fulfilled
            // We make an exception here to allow running the app on emulators
            showUnfulfilledRequirementsToast(report)
        }

        // Configure the Gini Capture SDK
        configureGiniCapture()
        val intent = Intent(this, CameraScreenApiActivity::class.java)
        startActivityForResult(intent, REQUEST_SCAN)
    }

    private fun configureGiniCapture() {
        val app = application as ScreenApiExampleApp
        app.clearGiniCaptureNetworkInstances()
        val builder = GiniCapture.newInstance(this)
            .setGiniCaptureNetworkService(
                giniCaptureDefaultNetworkService
            )
            .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
            .setFileImportEnabled(true)
            .setQRCodeScanningEnabled(configuration.isQrCodeEnabled)
            .setMultiPageEnabled(configuration.isMultiPageEnabled)
        builder.setFlashButtonEnabled(configuration.isFlashToggleEnabled)
        builder.setEventTracker(GiniCaptureEventTracker())
        builder.setCustomErrorLoggerListener(CustomErrorLoggerListener())
        builder.setReviewBottomBarNavigationAdapter(DefaultReviewNavigationBarBottomAdapter())
        builder.setLoadingIndicatorAdapter(DefaultLoadingIndicatorAdapter())
        val customHelpItems: MutableList<Custom> = ArrayList()
        customHelpItems.add(
            Custom(
                R.string.custom_help_screen_title,
                Intent(this, CustomHelpActivity::class.java)
            )
        )
        builder.setCustomHelpItems(customHelpItems)
        builder.setBottomNavigationBarEnabled(configuration.isBottomNavigationBarEnabled)
        if (/*animatedOnboardingIllustrationsSwitch!!.isChecked*/true) {
            builder.setOnboardingAlignCornersIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    resources.getIdentifier("floating_document", "raw", this.packageName)
                )
            )
            builder.setOnboardingLightingIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    resources.getIdentifier("lighting", "raw", this.packageName)
                )
            )
            builder.setOnboardingMultiPageIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    resources.getIdentifier("multipage", "raw", this.packageName)
                )
            )
            builder.setOnboardingQRCodeIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    R.raw.scan_qr_code
                )
            )
        }
        //TODO: set from configuration object
        if (/*customLoadingAnimationSwitch!!.isChecked*/true) {
            builder.setLoadingIndicatorAdapter(
                CustomLottiLoadingIndicatorAdapter(
                    resources.getIdentifier("custom_loading", "raw", this.packageName)
                )
            )
        }
        builder.setOnlyQRCodeScanning(configuration.isOnlyQrCodeEnabled)
        builder.build()
    }

    private fun showUnfulfilledRequirementsToast(report: RequirementsReport) {
        val stringBuilder = StringBuilder()
        val requirementReports = report.requirementReports
        for (i in requirementReports.indices) {
            val requirementReport = requirementReports[i]
            if (!requirementReport.isFulfilled) {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append("\n")
                }
                stringBuilder.append(requirementReport.requirementId)
                if (requirementReport.details.isNotEmpty()) {
                    stringBuilder.append(": ")
                    stringBuilder.append(requirementReport.details)
                }
            }
        }
        Toast.makeText(
            this, "Requirements not fulfilled:\n$stringBuilder",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun getOnboardingPages(
        isMultiPageEnabled: Boolean,
        isQRCodeScanningEnabled: Boolean
    ): ArrayList<OnboardingPage> {
        // Adding a custom page to the default pages
        val pages = asArrayList(isMultiPageEnabled, isQRCodeScanningEnabled)
        pages.add(
            OnboardingPage(
                R.string.additional_onboarding_page_title,
                R.string.additional_onboarding_page_message,
                null
            )
        )
        return pages
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCAN) {
            if (data == null) {
                if (isIntentActionViewOrSend(intent)) {
                    finish()
                }
                if (resultCode == CameraActivity.RESULT_ENTER_MANUALLY) {
                    handleEnterManuallyAction()
                }
                return
            }
            when (resultCode) {
                CameraActivity.RESULT_ENTER_MANUALLY -> handleEnterManuallyAction()
                RESULT_CANCELED -> {}
                RESULT_OK -> {
                    // Retrieve the extractions
                    var extractionsBundle = data.getBundleExtra(
                        CameraActivity.EXTRA_OUT_EXTRACTIONS
                    )
                    if (extractionsBundle == null) {
                        extractionsBundle = data.getBundleExtra(EXTRA_OUT_EXTRACTIONS)
                    }
                    if (extractionsBundle == null) {
                        if (isIntentActionViewOrSend(intent)) {
                            finish()
                        }
                        return
                    }
                    val compoundExtractionsBundle =
                        data.getBundleExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS)
                    if ((pay5ExtractionsAvailable(extractionsBundle)
                                || epsPaymentAvailable(extractionsBundle)) || compoundExtractionsBundle != null
                    ) {
                        startExtractionsActivity(extractionsBundle, compoundExtractionsBundle)
                    }
                }

                CameraActivity.RESULT_ERROR -> {
                    // Something went wrong, retrieve and show the error
                    val error = data.getParcelableExtra<GiniCaptureError>(
                        CameraActivity.EXTRA_OUT_ERROR
                    )
                    if (error != null) {
                        Toast.makeText(
                            this, "Error: "
                                    + error.errorCode + " - "
                                    + error.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            if (isIntentActionViewOrSend(intent)) {
                finish()
            }
        }
        if (requestCode == REQUEST_CONFIGURATION) {
            when (resultCode) {
                RESULT_CANCELED -> {}
                RESULT_OK -> {
                    var configurationResult: Configuration? = data?.getParcelableExtra(
                        CONFIGURATION_BUNDLE
                    )


                    if (configurationResult != null) {
                        configuration = configurationResult
                        configurationViewModel.setConfiguration(configurationResult)
                    }
                }
            }
        }
    }

    private fun pay5ExtractionsAvailable(extractionsBundle: Bundle): Boolean {
        for (key in extractionsBundle.keySet()) {
            if (ExampleUtil.isPay5Extraction(key)) {
                return true
            }
        }
        return false
    }

    private fun epsPaymentAvailable(extractionsBundle: Bundle): Boolean {
        for (key in extractionsBundle.keySet()) {
            if (key == "epsPaymentQRCodeUrl") {
                return true
            }
        }
        return false
    }

    private fun startExtractionsActivity(
        extractionsBundle: Bundle,
        compoundExtractionsBundle: Bundle?
    ) {
        val intent = Intent(this, ExtractionsActivity::class.java)
        intent.putExtra(ExtractionsActivity.EXTRA_IN_EXTRACTIONS, extractionsBundle)
        intent.putExtra(
            ExtractionsActivity.EXTRA_IN_COMPOUND_EXTRACTIONS,
            compoundExtractionsBundle
        )
        startActivity(intent)
    }

    private fun configureLogging() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.reset()
        val layoutEncoder = PatternLayoutEncoder()
        layoutEncoder.context = lc
        layoutEncoder.pattern = "%-5level %file:%line [%thread] - %msg%n"
        layoutEncoder.start()
        val logcatAppender = LogcatAppender()
        logcatAppender.context = lc
        logcatAppender.encoder = layoutEncoder
        logcatAppender.start()
        val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(logcatAppender)
    }

    private fun handleEnterManuallyAction() {
        Toast.makeText(this, "Scan exited for manual enter mode", Toast.LENGTH_SHORT).show()
    }

    private class GiniCaptureEventTracker : EventTracker {
        override fun onOnboardingScreenEvent(event: Event<OnboardingScreenEvent>) {
            when (event.type) {
                OnboardingScreenEvent.START -> LOG.info("Onboarding started")
                OnboardingScreenEvent.FINISH -> LOG.info("Onboarding finished")
            }
        }

        override fun onCameraScreenEvent(event: Event<CameraScreenEvent>) {
            when (event.type) {
                CameraScreenEvent.TAKE_PICTURE -> LOG.info("Take picture")
                CameraScreenEvent.HELP -> LOG.info("Show help")
                CameraScreenEvent.EXIT -> LOG.info("Exit")
            }
        }

        override fun onReviewScreenEvent(event: Event<ReviewScreenEvent>) {
            when (event.type) {
                ReviewScreenEvent.NEXT -> LOG.info("Go next to analyse")
                ReviewScreenEvent.BACK -> LOG.info("Go back to the camera")
                ReviewScreenEvent.UPLOAD_ERROR -> {
                    val error =
                        event.details[ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
                    LOG.info(
                        "Upload failed:\nmessage: {}\nerror:",
                        event.details[ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE],
                        error
                    )
                }
            }
        }

        override fun onAnalysisScreenEvent(event: Event<AnalysisScreenEvent>) {
            when (event.type) {
                AnalysisScreenEvent.ERROR -> {
                    val error =
                        event.details[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
                    LOG.info(
                        "Analysis failed:\nmessage: {}\nerror:",
                        event.details[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE],
                        error
                    )
                }

                AnalysisScreenEvent.RETRY -> LOG.info("Retry analysis")
                AnalysisScreenEvent.CANCEL -> LOG.info("Analysis cancelled")
            }
        }
    }

    private class CustomErrorLoggerListener : ErrorLoggerListener {
        override fun handleErrorLog(errorLog: ErrorLog) {
            LOG.error("Custom error logger: {}", errorLog.toString())
        }
    }

    companion object {
        const val EXTRA_OUT_EXTRACTIONS = "EXTRA_OUT_EXTRACTIONS"
        const val CONFIGURATION_BUNDLE = "CONFIGURATION_BUNDLE"
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
        private const val REQUEST_SCAN = 1
        private const val REQUEST_NO_EXTRACTIONS = 2
        private const val REQUEST_CONFIGURATION = 3
    }
}