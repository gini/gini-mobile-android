package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureFlowFragmentListener
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.bank.sdk.exampleapp.core.di.GiniCaptureNetworkServiceDebugEnabled
import net.gini.android.capture.CaptureResult
import net.gini.android.capture.Document
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.ResultError
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import net.gini.android.core.api.DocumentMetadata
import javax.inject.Inject

@AndroidEntryPoint
class ClientGiniCaptureFragment : Fragment(R.layout.fragment_client_capture),
    //Bank SDK
    CaptureFlowFragmentListener {
    //Capture SDK
//    GiniCaptureFragmentListener,  {

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
//                configureCaptureSDK()
            } else {
                if (intent != null) {
                    requireActivity().finish()
                }
            }
        }
    }


    private fun configureCaptureSDK() {
        val builder = GiniCapture.newInstance(requireContext())
            .setGiniCaptureNetworkService(
                giniCaptureDefaultNetworkService
            )
            .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
            .setFileImportEnabled(true)
            .setQRCodeScanningEnabled(true)
            .setMultiPageEnabled(true)
        builder.setFlashButtonEnabled(true)
        builder.setReviewBottomBarNavigationAdapter(DefaultReviewNavigationBarBottomAdapter())
        builder.setLoadingIndicatorAdapter(DefaultLoadingIndicatorAdapter())

        builder.build()
        startCaptureSDK()
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



    private fun startCaptureSDK() {
        val giniCaptureFragment = GiniCapture.createGiniCaptureFragment()
        //giniCaptureFragment.setListener(this)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_host, giniCaptureFragment, "fragment_host")
            .addToBackStack(null)
            .commit()
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


