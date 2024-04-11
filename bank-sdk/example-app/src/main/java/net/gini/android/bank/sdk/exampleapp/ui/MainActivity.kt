package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureFlowContract
import net.gini.android.bank.sdk.capture.CaptureFlowImportContract
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityMainBinding
import net.gini.android.bank.sdk.exampleapp.ui.data.Configuration
import net.gini.android.capture.Document
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.util.CancellationToken

/**
 * Entry point for the screen api example app.
 */

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val configurationViewModel: ConfigurationViewModel by viewModels()
    private val captureLauncher =
        registerForActivityResult(CaptureFlowContract(), ::onCaptureResult)
    private val captureImportLauncher =
        registerForActivityResult(CaptureFlowImportContract(), ::onCaptureResult)
    private var cancellationToken: CancellationToken? =
        null // should be kept across configuration changes
    private val permissionHandler = PermissionHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addInputHandlers()
        showVersions()
        if (savedInstanceState == null) {
            if (isIntentActionViewOrSend(intent)) {
                // For "open with" (file import) tests
                (applicationContext as ExampleApp).idlingResourceForOpenWith.increment()

                startGiniBankSdkForOpenWith(intent)
            } else if (intent.hasExtra(EXTRA_IN_OPEN_WITH_DOCUMENT)) {
                IntentCompat.getParcelableExtra(intent, EXTRA_IN_OPEN_WITH_DOCUMENT, Document::class.java)?.let {
                    // Launch the Bank SDK with a delay to allow the SplashActivity to finish.
                    // This will lead to a PermissionDenied exception, if the files received through "open with"
                    // were not loaded into memory before the SplashActivity was finished.
                    binding.root.postDelayed({
                        startCaptureFlowForDocument(it)
                    }, 600)
                }
            }
        }
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
            startGiniBankSdkForOpenWith(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showVersions() {
        binding.textGiniBankVersion.text =
                getString(R.string.gini_bank_sdk_version) + net.gini.android.bank.sdk.BuildConfig.VERSION_NAME +
                getString(R.string.gini_capture_sdk_version) + net.gini.android.capture.BuildConfig.VERSION_NAME +
                getString(R.string.gini_client_id) +  getString(R.string.gini_api_client_id)

    }

    private fun addInputHandlers() {
        binding.buttonStartScanner.setOnClickListener {
            checkIfAppShouldAskForCameraPermission(EntryPoint.BUTTON)
        }

        binding.tilFieldEntryPoint.setEndIconOnClickListener {
            checkIfAppShouldAskForCameraPermission(EntryPoint.FIELD)

        }

        binding.buttonStartSingleActivity.setOnClickListener {
            configureGiniBank()
            startActivity(CaptureFlowHostActivity.newIntent(this))
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

    private fun checkIfAppShouldAskForCameraPermission(entryPoint: EntryPoint) {
        configurationViewModel.setConfiguration(
            configurationViewModel.configurationFlow.value.copy(
                entryPoint = entryPoint
            )
        )
        if (configurationViewModel.disableCameraPermissionFlow.value) {
            startGiniBankSdk()
        } else {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission(intent: Intent? = null) {
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                startGiniBankSdk()
            } else {
                if (intent != null) {
                    finish()
                }
            }
        }
    }

    private fun startGiniBankSdkForOpenWith(openWithIntent: Intent) {
        if (configurationViewModel.configurationFlow.value.isFileImportEnabled) {
            configureGiniBank()
            startGiniBankSdk(openWithIntent)
        } else {
            MaterialAlertDialogBuilder(this).setMessage(R.string.file_import_feature_is_disabled_dialog_message)
                .setPositiveButton("OK") { dialogInterface, i -> {} }.show()
        }
    }

    private fun startGiniBankSdk(intent: Intent? = null) {
        configureGiniBank()

        if (intent != null) {
            cancellationToken = GiniBank.startCaptureFlowForIntent(
                captureImportLauncher, this@MainActivity, intent
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

    private fun configureGiniBank() {
        configurationViewModel.clearGiniCaptureNetworkInstances()
        configurationViewModel.configureGiniBank(this)
    }

    private fun startCaptureFlowForDocument(document: Document) {
        GiniBank.startCaptureFlowForDocument(
            resultLauncher = captureImportLauncher,
            document = document
        )
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


    companion object {
        const val CONFIGURATION_BUNDLE = "CONFIGURATION_BUNDLE"
        const val CAMERA_PERMISSION_BUNDLE = "CAMERA_PERMISSION_BUNDLE"
        const val EXTRA_IN_OPEN_WITH_DOCUMENT = "EXTRA_IN_OPEN_WITH_DOCUMENT"
        private const val REQUEST_CONFIGURATION = 3
    }
}