package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureFlowFragmentListener
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.bank.sdk.exampleapp.core.di.GiniCaptureNetworkServiceDebugEnabled
import net.gini.android.capture.CaptureSDKResultError
import net.gini.android.capture.Document
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata
import javax.inject.Inject

@AndroidEntryPoint
class ClientGiniCaptureFragment :
    Fragment(R.layout.fragment_client_capture),
    CaptureFlowFragmentListener {

    @Inject
    @GiniCaptureNetworkServiceDebugEnabled
    lateinit var giniCaptureDefaultNetworkService: GiniCaptureDefaultNetworkService
    private lateinit var permissionHandler: PermissionHandler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        checkCameraPermission()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun checkCameraPermission(intent: Intent? = null) {
        permissionHandler = PermissionHandler(requireActivity())
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                configureBankSDK()
            } else {
                if (intent != null) {
                    requireActivity().finish()
                }
            }
        }
    }


    private fun configureBankSDK() {
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
        GiniBank.enableReturnReasons = true

        startBankSDK()
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
        configureBankSDK()
//        GiniBank.createCaptureFlowFragmentForIntent(requireContext(), openWithIntent) { result ->
//            when (result) {
//                GiniBank.CreateCaptureFlowFragmentForIntentResult.Cancelled -> requireActivity().finish()
//                is GiniBank.CreateCaptureFlowFragmentForIntentResult.Error -> Toast.makeText(
//                    requireContext(),
//                    "Open with failed with error ${result.exception.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//                is GiniBank.CreateCaptureFlowFragmentForIntentResult.Success -> {
//                    result.fragment.setListener(this)
//
//                    requireActivity().supportFragmentManager.beginTransaction()
//                        .replace(R.id.fragment_host, result.fragment, "gc_fragment_host")
//                        .addToBackStack(null)
//                        .commit()
//                }
//            }
//        }

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
                    is CaptureResult ->
                        Toast.makeText(
                            requireContext(),
                            "Error: ${(result.value as CaptureSDKResultError.Capture).giniCaptureError.errorCode} ${(result.value as CaptureSDKResultError.Capture).giniCaptureError.message}",
                            Toast.LENGTH_LONG
                        ).show()

                    is ResultError.FileImport ->
                        Toast.makeText(
                            requireContext(),
                            "Error: ${(result.value as CaptureSDKResultError.FileImport).code} ${(result.value as CaptureSDKResultError.FileImport).message}",
                            Toast.LENGTH_LONG
                        ).show()

                    else -> {}
                }
                //if (isIntentActionViewOrSend(requireActivity().intent)) {
                requireActivity().finish()
                // }
            }

            CaptureResult.Empty -> {
                //if (isIntentActionViewOrSend(requireActivity().intent)) {
                requireActivity().finish()
                //}
            }

            CaptureResult.Cancel -> {
                //if (isIntentActionViewOrSend(requireActivity().intent)) {
                requireActivity().finish()
                //}
            }

            CaptureResult.EnterManually -> {
                Toast.makeText(
                    requireContext(),
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                if (isIntentActionViewOrSend(requireActivity().intent)) {
                    requireActivity().finish()
                }
            }
        }
    }


    override fun onFinishedWithCancellation() {
        requireActivity().finish()
    }

    override fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        TODO("Not yet implemented")
    }


}


