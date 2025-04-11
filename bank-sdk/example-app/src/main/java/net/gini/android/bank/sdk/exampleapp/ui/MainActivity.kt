package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
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
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.TransactionListActivity
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
    private var configurationActivityLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addInputHandlers()
        setupActivityResultLauncher()
        showVersions()
        if (savedInstanceState == null) {
            if (isIntentActionViewOrSend(intent)) {
                startGiniBankSdkForOpenWith(intent)
            } else if (intent.hasExtra(EXTRA_IN_OPEN_WITH_DOCUMENT)) {
                IntentCompat.getParcelableExtra(
                    intent,
                    EXTRA_IN_OPEN_WITH_DOCUMENT,
                    Document::class.java
                )?.let {
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

    override fun onNewIntent(intent: Intent) {
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
                    getString(R.string.gini_client_id) + getString(R.string.gini_api_client_id)

    }

    private fun setupActivityResultLauncher() {
        configurationActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                when (result.resultCode) {
                    RESULT_CANCELED -> {}
                    RESULT_OK -> {
                        val configurationResult: Configuration? = result.data?.getParcelableExtra(
                            CONFIGURATION_BUNDLE
                        )
                        if (configurationResult != null) {
                            configurationViewModel.setConfiguration(configurationResult)
                        }

                        configurationViewModel.disableCameraPermission(
                            result.data?.getBooleanExtra(
                                CAMERA_PERMISSION_BUNDLE, false
                            ) ?: false
                        )
                    }
                }
            }
    }

    private fun addInputHandlers() {
        binding.buttonStartScanner.setOnClickListener {
            checkIfAppShouldAskForCameraPermission(EntryPoint.BUTTON)
        }

        binding.tilFieldEntryPoint.setEndIconOnClickListener {
            checkIfAppShouldAskForCameraPermission(EntryPoint.FIELD)
        }

        binding.buttonOpenTlDemo.setOnClickListener {
            startActivity(Intent(this, TransactionListActivity::class.java))
        }

        binding.buttonStartSingleActivity.setOnClickListener {
            configureGiniBank()
            startActivity(CaptureFlowHostActivity.newIntent(this))
        }

        binding.textGiniBankVersion.setOnClickListener {
            configurationActivityLauncher?.launch(
                Intent(this, ConfigurationActivity::class.java)
                    .putExtra(
                        CONFIGURATION_BUNDLE,
                        configurationViewModel.configurationFlow.value
                    )
                    .putExtra(
                        CAMERA_PERMISSION_BUNDLE,
                        configurationViewModel.disableCameraPermissionFlow.value
                    )
            )
        }
    }

    private fun checkIfAppShouldAskForCameraPermission(entryPoint: EntryPoint) {
        configurationViewModel.setConfiguration(
            configurationViewModel.configurationFlow.value.copy(
                entryPoint = entryPoint
            )
        )

        askCameraPermissionAndRun {
            startGiniBankSdk()
        }
    }

    private fun askCameraPermissionAndRun(action: (Boolean) -> Unit) {
        lifecycleScope.launch {
            action(permissionHandler.grantPermission(Manifest.permission.CAMERA))
        }
    }

    private fun startGiniBankSdkForOpenWith(openWithIntent: Intent) {
        if (configurationViewModel.configurationFlow.value.isFileImportEnabled) {
            // For "open with" (file import) tests
            (applicationContext as ExampleApp).incrementIdlingResourceForOpenWith()

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


    companion object {
        const val CONFIGURATION_BUNDLE = "CONFIGURATION_BUNDLE"
        const val CAMERA_PERMISSION_BUNDLE = "CAMERA_PERMISSION_BUNDLE"
        const val EXTRA_IN_OPEN_WITH_DOCUMENT = "EXTRA_IN_OPEN_WITH_DOCUMENT"
        private const val REQUEST_CONFIGURATION = 3
    }
}