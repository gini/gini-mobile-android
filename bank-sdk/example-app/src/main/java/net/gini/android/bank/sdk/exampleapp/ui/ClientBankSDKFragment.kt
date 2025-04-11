package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureFlowFragment
import net.gini.android.bank.sdk.capture.CaptureFlowFragmentListener
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata

@AndroidEntryPoint
class ClientBankSDKFragment :
    Fragment(R.layout.fragment_client_bank_sdk),
    CaptureFlowFragmentListener {

    private lateinit var permissionHandler: PermissionHandler
    private val configurationViewModel: ConfigurationViewModel by activityViewModels()
    private var wasCameraPermissionGranted = false

    private fun handleOpenWithIntent(intent: Intent?) {
        val captureFlowFragment =
            requireActivity().supportFragmentManager.findFragmentByTag("captureFlowFragment") as? CaptureFlowFragment
        captureFlowFragment?.setListener(this)
        configureGiniBank()
        val openWithIntent =
            intent ?: requireArguments().getParcelable<Intent>("fileIntent")
        openWithIntent?.let { startBankSdkForIntent(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            handleOpenWithIntent(null)
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        checkCameraPermissionAndStartBankSdk()
    }

    fun checkCameraPermissionAndStartBankSdk() {
        permissionHandler = PermissionHandler(this)
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                // Bank SDK is configured in the MainActivity, but you can
                // call [overrideBankSDKConfiguration] here if you want to override the configuration
                configureGiniBank()
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

        handleOnBackPressed()

    }

    private fun showNoCameraPermissionMessage() {
        view?.findViewById<TextView>(R.id.no_camera_permission_message)?.visibility = View.VISIBLE
    }

    private fun hideNoCameraPermissionMessage() {
        view?.findViewById<TextView>(R.id.no_camera_permission_message)?.visibility = View.GONE
    }

    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                openMainFragment()
            }
        })
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
        childFragmentManager.beginTransaction()
            .replace(R.id.client_sdk, captureFlowFragment, "captureFlowFragment")
            .addToBackStack(null)
            .commit()
    }

    fun startBankSdkForIntent(openWithIntent: Intent) {
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

                    childFragmentManager.beginTransaction()
                        .replace(R.id.client_sdk, result.fragment, "captureFlowFragment")
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

    }


    private fun configureGiniBank() {
        configurationViewModel.clearGiniCaptureNetworkInstances()
        configurationViewModel.configureGiniBank(requireContext())
    }


    override fun onFinishedWithResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> {
                configurationViewModel.extractionsBundle = result.specificExtractions
                findNavController().navigate(
                    ClientBankSDKFragmentDirections.actionClientBankSDKFragmentToExtractionsFragment()
                )
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
                openMainFragment()
            }

            CaptureResult.Empty -> {
                openMainFragment()
            }

            CaptureResult.Cancel -> {
                openMainFragment()
            }

            CaptureResult.EnterManually -> {
                Toast.makeText(
                    requireContext(),
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                openMainFragment()
            }
        }
    }

    private fun openMainFragment() {
        findNavController().navigate(R.id.mainFragment)
    }


}


