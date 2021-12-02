package net.gini.android.bank.sdk.componentapiexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.gini.android.core.api.DocumentMetadata
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.network.GiniCaptureNetworkApi
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.requirements.RequirementsReport
import net.gini.android.bank.sdk.componentapiexample.camera.CameraExampleActivity
import net.gini.android.bank.sdk.componentapiexample.databinding.ActivityMainBinding
import net.gini.android.bank.sdk.componentapiexample.util.PermissionHandler
import net.gini.android.bank.sdk.componentapiexample.util.isIntentActionViewOrSend
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.network.getDefaultNetworkApi
import net.gini.android.bank.sdk.network.getDefaultNetworkService
import net.gini.android.bank.sdk.componentapiexample.BuildConfig

class MainActivity : AppCompatActivity() {

    private val permissionHandler = PermissionHandler(this)

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
        val (networkService, networkApi) = getNetworkService()
        GiniBank.setCaptureConfiguration(
            CaptureConfiguration(
                networkService = networkService,
                networkApi = networkApi,
                documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF_AND_IMAGES,
                fileImportEnabled = true,
                qrCodeScanningEnabled = true,
                multiPageEnabled = true,
                flashButtonEnabled = true,
                eventTracker = GiniCaptureEventTracker
            )
        )
    }

    private fun getNetworkService(): Pair<GiniCaptureNetworkService, GiniCaptureNetworkApi> {
        val clientId = getString(R.string.gini_api_client_id)
        val clientSecret = getString(R.string.gini_api_client_secret)
        val domain = "example.com"
        val documentMetadata = DocumentMetadata().apply {
            setBranchId("GiniBankExampleAndroid")
            add("AppFlow", "ComponentAPI")
        }
        val network = getDefaultNetworkService(this, clientId, clientSecret, domain, documentMetadata)
        return network to getDefaultNetworkApi(network)
    }

    private fun startGiniCaptureSdk(intent: Intent? = null) {
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                val report = GiniBank.checkCaptureRequirements(this@MainActivity)
                if (!report.isFulfilled) {
                    showUnfulfilledRequirementsToast(report)
                }
                configureGiniCapture()

                if (intent != null) {
                    startActivity(Intent(intent).apply {
                        setClass(this@MainActivity, CameraExampleActivity::class.java)
                    })
                } else {
                    startActivity(Intent(this@MainActivity, CameraExampleActivity::class.java))
                }
            } else {
                if (intent != null) {
                    finish()
                }
            }
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
}