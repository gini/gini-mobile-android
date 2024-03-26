package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureFlowFragment
import net.gini.android.bank.sdk.capture.CaptureFlowFragmentListener
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.capture.Document
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata

class ClientBankSDKFragment :
    Fragment(R.layout.fragment_client),
    CaptureFlowFragmentListener {

    private lateinit var permissionHandler: PermissionHandler

    private var wasCameraPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            checkCameraPermission()
        } else {
            val captureFlowFragment =
                requireActivity().supportFragmentManager.findFragmentByTag("fragment_host") as? CaptureFlowFragment
            captureFlowFragment?.setListener(this)
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    private fun checkCameraPermission() {
        permissionHandler = PermissionHandler(requireActivity())
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                // Bank SDK is configured in the MainActivity, but you can
                // call [overrideBankSDKConfiguration] here if you want to override the configuration
                startBankSDK()
                wasCameraPermissionGranted = true
                hideNoCameraPermissionMessage()
            } else {
                wasCameraPermissionGranted = false
                showNoCameraPermissionMessage()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (wasCameraPermissionGranted) {
            hideNoCameraPermissionMessage()
        } else {
            showNoCameraPermissionMessage()
        }
    }

    private fun showNoCameraPermissionMessage() {
        view?.findViewById<TextView>(R.id.no_camera_permission_message)?.visibility = View.VISIBLE
    }

    private fun hideNoCameraPermissionMessage() {
        view?.findViewById<TextView>(R.id.no_camera_permission_message)?.visibility = View.GONE
    }

    private fun overrideBankSDKConfiguration() {
        val clientId = requireContext().getString(R.string.gini_api_client_id)
        val clientSecret = requireContext().getString(R.string.gini_api_client_secret)
        val documentMetadata = DocumentMetadata()
        documentMetadata.setBranchId("GCSExampleAndroid")
        documentMetadata.add("AppFlow", "ScreenAPI")

        val networkService = GiniCaptureDefaultNetworkService
            .builder(requireContext())
            .setClientCredentials(
                clientId,
                clientSecret,
                "example.com"
            )
            .setDocumentMetadata(documentMetadata)
            .build()

        val captureConfiguration = CaptureConfiguration(
            networkService = networkService,
            fileImportEnabled = true,
            documentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF_AND_IMAGES,
            qrCodeScanningEnabled = true,
            flashButtonEnabled = true,
            multiPageEnabled = true,
        )
        GiniBank.setCaptureConfiguration(requireContext(), captureConfiguration)

    }


    private fun startBankSDK() {
        val captureFlowFragment = GiniBank.createCaptureFlowFragment()
        captureFlowFragment.setListener(this)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_host, captureFlowFragment, "fragment_host")
            .addToBackStack(null)
            .commit()
    }

    fun startBankSDKForIntent(openWithIntent: Intent) {
        // Bank SDK is configured in the MainActivity, but you can
        // call [overrideBankSDKConfiguration] here if you want to override the configuration
        GiniBank.createCaptureFlowFragmentForIntent(requireContext(), openWithIntent) { result ->
            when (result) {
                GiniBank.CreateCaptureFlowFragmentForIntentResult.Cancelled -> requireActivity().finish()
                is GiniBank.CreateCaptureFlowFragmentForIntentResult.Error -> Toast.makeText(
                    requireContext(),
                    "Open with failed with error ${result.exception.message}",
                    Toast.LENGTH_SHORT
                ).show()

                is GiniBank.CreateCaptureFlowFragmentForIntentResult.Success -> {
                    result.fragment.setListener(this)

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_host, result.fragment, "fragment_host")
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

    }

    fun startBankSDKForDocument(document: Document) {
        val fragment = GiniBank.createCaptureFlowFragmentForDocument(document)
        fragment.setListener(this)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_host, fragment, "fragment_host")
            .addToBackStack(null)
            .commit()
    }

    override fun onFinishedWithResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> {
                startActivity(
                    ExtractionsActivity.getStartIntent(
                        requireContext(),
                        result.specificExtractions
                    )
                )
                requireActivity().finish()
            }

            is CaptureResult.Error -> {
                when (result.value) {
                    is ResultError.Capture ->
                        Toast.makeText(
                            requireContext(),
                            "Error: ${(result.value as ResultError.Capture).giniCaptureError.errorCode} ${(result.value as ResultError.Capture).giniCaptureError.message}",
                            Toast.LENGTH_LONG
                        ).show()

                    is ResultError.FileImport ->
                        Toast.makeText(
                            requireContext(),
                            "Error: ${(result.value as ResultError.FileImport).code} ${(result.value as ResultError.FileImport).message}",
                            Toast.LENGTH_LONG
                        ).show()

                    else -> {}
                }
                requireActivity().finish()
            }

            CaptureResult.Empty -> {
                requireActivity().finish()
            }

            CaptureResult.Cancel -> {
                requireActivity().finish()
            }

            CaptureResult.EnterManually -> {
                Toast.makeText(
                    requireContext(),
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().finish()
            }
        }
    }



}


