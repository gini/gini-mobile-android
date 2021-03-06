package net.gini.android.bank.sdk.screenapiexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.network.GiniCaptureDefaultNetworkApi
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.capture.util.CancellationToken
import net.gini.android.bank.sdk.screenapiexample.databinding.ActivityMainBinding
import net.gini.android.bank.sdk.screenapiexample.util.PermissionHandler
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.*
import net.gini.android.bank.sdk.screenapiexample.BuildConfig
import net.gini.android.bank.sdk.screenapiexample.R
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val permissionHandler = PermissionHandler(this)
    private val captureLauncher = registerForActivityResult(CaptureFlowContract(), ::onCaptureResult)
    private val captureImportLauncher = registerForActivityResult(CaptureFlowImportContract(), ::onCaptureResult)
    private val noExtractionsLauncher = registerForActivityResult(NoExtractionContract(), ::onStartAgainResult)
    private var cancellationToken: CancellationToken? = null // should be kept across configuration changes
    private val networkService: GiniCaptureDefaultNetworkService by inject()
    private val networkApi: GiniCaptureDefaultNetworkApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showVersions(binding)
        setViewListeners(binding)
        if (savedInstanceState == null) {
            if (isIntentActionViewOrSend(intent)) {
                startGiniCaptureSdk(intent)
            }
        }
    }

    private fun configureGiniCapture() {
        GiniBank.releaseCapture(this)
        GiniBank.setCaptureConfiguration(
            CaptureConfiguration(
                networkService = networkService,
                networkApi = networkApi,
                documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF_AND_IMAGES,
                fileImportEnabled = true,
                qrCodeScanningEnabled = true,
                multiPageEnabled = true,
                flashButtonEnabled = true,
                eventTracker = GiniCaptureEventTracker,
                customHelpItems = listOf(
                    HelpItem.Custom(
                        R.string.custom_help_screen_title,
                        Intent(this, CustomHelpActivity::class.java)
                    )
                ),
                importedFileSizeBytesLimit = 5 * 1024 * 1024
            )
        )
    }

    private fun configureGiniBank() {
        GiniBank.enableReturnReasons = true
    }

    private fun startGiniCaptureSdk(intent: Intent? = null) {
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                val report = GiniBank.checkCaptureRequirements(this@MainActivity)
                if (!report.isFulfilled) {
                    showUnfulfilledRequirementsToast(report)
                }
                configureGiniBank()
                configureGiniCapture()

                if (intent != null) {
                    cancellationToken = GiniBank.startCaptureFlowForIntent(captureImportLauncher, this@MainActivity, intent)
                } else {
                    GiniBank.startCaptureFlow(captureLauncher)
                }
            } else {
                if (intent != null) {
                    finish()
                }
            }
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
            }
            CaptureResult.Empty -> {
                noExtractionsLauncher.launch(Unit)
            }
            CaptureResult.Cancel -> {
            }
        }
    }

    private fun onStartAgainResult(startAgain: Boolean) {
        if (startAgain) {
            GiniBank.startCaptureFlow(captureLauncher)
        }
    }

    private fun showUnfulfilledRequirementsToast(reports: RequirementsReport) {
        reports.requirementReports.joinToString(separator = "\n") { report ->
            if (!report.isFulfilled) {
                "${report.requirementId}: ${report.details}"
            } else ""
        }.also { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setViewListeners(binding: ActivityMainBinding) {
        binding.buttonStartScanner.setOnClickListener {
            startGiniCaptureSdk()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showVersions(binding: ActivityMainBinding) {
        binding.textGiniBankVersion.text = "Gini Bank SDK v${net.gini.android.bank.sdk.BuildConfig.VERSION_NAME}"
        binding.textAppVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun isIntentActionViewOrSend(intent: Intent): Boolean =
        Intent.ACTION_VIEW == intent.action || Intent.ACTION_SEND == intent.action || Intent.ACTION_SEND_MULTIPLE == intent.action

    override fun onDestroy() {
        super.onDestroy()
        // cancellationToken shouldn't be canceled when activity is recreated.
        // For example cancel in ViewModel's onCleared() instead.
        cancellationToken?.cancel()
    }
}