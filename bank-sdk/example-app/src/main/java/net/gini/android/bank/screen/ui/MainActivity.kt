package net.gini.android.bank.screen.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.screen.BuildConfig
import net.gini.android.bank.screen.ExampleApp
import net.gini.android.bank.screen.R
import net.gini.android.bank.screen.core.PermissionHandler
import net.gini.android.bank.screen.databinding.ActivityMainBinding
import net.gini.android.bank.screen.ui.data.Configuration
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureFlowContract
import net.gini.android.bank.sdk.capture.CaptureFlowImportContract
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.capture.GiniCaptureDebug
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.util.CancellationToken
import org.slf4j.LoggerFactory


/**
 * Entry point for the screen api example app.
 */

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val configurationViewModel: ConfigurationViewModel by viewModels()
    private val captureImportLauncher =
            registerForActivityResult(CaptureFlowImportContract(), ::onCaptureResult)
    private var cancellationToken: CancellationToken? =
            null // should be kept across configuration changes
    private val permissionHandler = PermissionHandler(this)
    private val captureLauncher =
            registerForActivityResult(CaptureFlowContract(), ::onCaptureResult)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addInputHandlers()
        setGiniCaptureSdkDebugging()
        showVersions()

    }


    override fun onDestroy() {
        super.onDestroy()
        // cancellationToken shouldn't be canceled when activity is recreated.
        // For example cancel in ViewModel's onCleared() instead.
        cancellationToken?.cancel()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && isIntentActionViewOrSend(intent)) {
            startGiniCaptureSdk(intent)
        }
    }


    private fun isIntentActionViewOrSend(intent: Intent): Boolean {
        val action = intent.action
        return Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action
    }

    @SuppressLint("SetTextI18n")
    private fun showVersions() {
        binding.textGiniBankVersion.text =
                getString(R.string.gini_capture_sdk_version) + net.gini.android.bank.sdk.BuildConfig.VERSION_NAME
    }

    private fun setGiniCaptureSdkDebugging() {
        if (BuildConfig.DEBUG) {
            GiniCaptureDebug.enable()
            configureLogging()
        }
    }

    private fun addInputHandlers() {
        binding.buttonStartScanner.setOnClickListener { v: View? ->
            if (configurationViewModel.disableCameraPermissionFlow.value) {
                startGiniCaptureSdk()
            } else {
                checkCameraPermission()
            }
        }

        binding.textGiniBankVersion.setOnClickListener {
            startActivityForResult(
                    Intent(
                            this, ConfigurationActivity::class.java
                    )
                            .putExtra(
                                    CONFIGURATION_BUNDLE,
                                    configurationViewModel.configurationFlow.value
                            )
                            .putExtra(
                                    CAMERA_PERMISSION_BUNDLE,
                                    configurationViewModel.disableCameraPermissionFlow.value
                            ),
                    REQUEST_CONFIGURATION
            )
        }

    }

    private fun checkCameraPermission(intent: Intent? = null) {
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                startGiniCaptureSdk()
            } else {
                if (intent != null) {
                    finish()
                }
            }
        }
    }

    private fun startGiniCaptureSdk(intent: Intent? = null) {
        val report = GiniBank.checkCaptureRequirements(this@MainActivity)
        if (!report.isFulfilled) {
            showUnfulfilledRequirementsToast(report)
        }

        configureGiniCapture()

        if (intent != null) {
            cancellationToken = GiniBank.startCaptureFlowForIntent(
                    captureImportLauncher,
                    this@MainActivity,
                    intent
            )
        } else {
            GiniBank.startCaptureFlow(captureLauncher)
        }
    }


    private fun onCaptureResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> {
                startActivity(ExtractionsActivity.getStartIntent(this, result.specificExtractions))
            }

            is CaptureResult.Error -> {
                when (result.value) {
                    is ResultError.Capture ->
                        Toast.makeText(
                                this,
                                "Error: ${(result.value as ResultError.Capture).giniCaptureError.errorCode} ${(result.value as ResultError.Capture).giniCaptureError.message}",
                                Toast.LENGTH_LONG
                        ).show()

                    is ResultError.FileImport ->
                        Toast.makeText(
                                this,
                                "Error: ${(result.value as ResultError.FileImport).code} ${(result.value as ResultError.FileImport).message}",
                                Toast.LENGTH_LONG
                        ).show()
                }
                if (isIntentActionViewOrSend(intent)) {
                    finish()
                }
            }

            CaptureResult.Empty -> {
                if (isIntentActionViewOrSend(intent)) {
                    finish()
                }
            }

            CaptureResult.Cancel -> {
                if (isIntentActionViewOrSend(intent)) {
                    finish()
                }
            }

            CaptureResult.EnterManually -> {
                Toast.makeText(this, "Scan exited for manual enter mode", Toast.LENGTH_SHORT).show()
                if (isIntentActionViewOrSend(intent)) {
                    finish()
                }
            }
        }
    }

    private fun configureGiniCapture() {
        val app = application as ExampleApp
        app.clearGiniCaptureNetworkInstances()
        configurationViewModel.configureGiniBank(this)
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
                this, "Requirements not fulfilled:\n$stringBuilder", Toast.LENGTH_LONG
        ).show()
    }

    override fun onActivityResult(
            requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURATION) {
            when (resultCode) {
                RESULT_CANCELED -> {}
                RESULT_OK -> {
                    var configurationResult: Configuration? = data?.getParcelableExtra(
                            CONFIGURATION_BUNDLE
                    )
                    if (configurationResult != null) {
                        configurationViewModel.setConfiguration(configurationResult)
                    }

                    configurationViewModel.disableCameraPermission(
                            data?.getBooleanExtra(
                                    CAMERA_PERMISSION_BUNDLE, false
                            ) ?: false
                    )
                }
            }
        }
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


    companion object {
        const val CONFIGURATION_BUNDLE = "CONFIGURATION_BUNDLE"
        const val CAMERA_PERMISSION_BUNDLE = "CAMERA_PERMISSION_BUNDLE"
        private const val REQUEST_CONFIGURATION = 3
    }
}