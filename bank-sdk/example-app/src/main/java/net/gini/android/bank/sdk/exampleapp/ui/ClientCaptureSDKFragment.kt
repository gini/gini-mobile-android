package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.capture.Amount
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.util.CancellationToken
import net.gini.android.core.api.DocumentMetadata

class ClientCaptureSDKFragment :
    Fragment(R.layout.fragment_client),
    GiniCaptureFragmentListener {

    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(requireContext(), "ClientCaptureSDKFragment", Toast.LENGTH_SHORT).show()
        if (savedInstanceState == null) {
            checkCameraPermission()
        } else {
            val giniCaptureFragment =
                requireActivity().supportFragmentManager.findFragmentByTag("fragment_host") as? GiniCaptureFragment
            giniCaptureFragment?.setListener(this)
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    private fun checkCameraPermission() {
        permissionHandler = PermissionHandler(requireActivity())
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                configureCaptureSDK()
                startCaptureSDK()
            }
        }
    }

    private fun configureCaptureSDK() {
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

        val capture = GiniCapture.Builder()
            .setGiniCaptureNetworkService(networkService)
            .setFileImportEnabled(true)
            .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
            .setQRCodeScanningEnabled(true)
            .setFlashButtonEnabled(true)
            .setMultiPageEnabled(true)
            .build()

    }


    private fun startCaptureSDK() {
        val giniCaptureFragment = GiniCapture.createGiniCaptureFragment()
        giniCaptureFragment.setListener(this)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_host, giniCaptureFragment, "fragment_host")
            .addToBackStack(null)
            .commit()
    }

    private var mFileImportCancellationToken: CancellationToken? = null

    fun startCaptureSDKForIntent(openWithIntent: Intent) {
        mFileImportCancellationToken = GiniCapture.getInstance().createGiniCaptureFragmentForIntent(
            requireActivity(),
            openWithIntent,
            object : GiniCapture.CreateGiniCaptureFragmentForIntentCallback {
                override fun callback(result: GiniCapture.CreateGiniCaptureFragmentForIntentResult?) {
                    when (result) {
                        is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Success -> {
                            // Opening the file(s) from the intent and creating the GiniCaptureFragment finished

                            // Set the listener to receive the Gini Bank SDK's results
                            result.fragment.setListener(this@ClientCaptureSDKFragment)

                            // Show the CaptureFlowFragment for example via the fragment manager:
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_host, result.fragment, "GiniCaptureFragment")
                                .addToBackStack(null)
                                .commit()
                        }

                        is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Error -> {
                        // Something went wrong when opening the file(s) from the intent or uploading the document


                        }

                        is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Cancelled -> {
                        }
                    }
                }

            })

    }

    fun stopGiniCaptureSDKWithTransferSummary(paymentRecipient: String,
                                           paymentReference: String,
                                           paymentPurpose: String,
                                           iban: String,
                                           bic: String,
                                           amount: Amount
    ) {
        // After the user has seen and potentially corrected the extractions, send the final
        // transfer summary values to Gini which will be used to improve the future extraction accuracy:
        GiniCapture.sendTransferSummary(
            paymentRecipient,
            paymentReference,
            paymentPurpose,
            iban,
            bic,
            amount
        )

        // cleanup the capture SDK after sending the transfer summary
        GiniCapture.cleanup(requireActivity())
    }

    override fun onFinishedWithResult(result: CaptureSDKResult) {
        when (result) {
            is CaptureSDKResult.Success -> {
                startActivity(
                    ExtractionsActivity.getStartIntent(
                        requireContext(),
                        result.specificExtractions
                    )
                )
                requireActivity().finish()
            }

            is CaptureSDKResult.Error -> {

                Toast.makeText(
                    requireContext(),
                    "Error: ${(result.value as ResultError.FileImport).code} ${(result.value as ResultError.FileImport).message}",
                    Toast.LENGTH_LONG
                ).show()

                requireActivity().finish()
            }

            CaptureSDKResult.Empty -> {
                requireActivity().finish()
            }

            CaptureSDKResult.Cancel -> {
                requireActivity().finish()
            }

            CaptureSDKResult.EnterManually -> {
                Toast.makeText(
                    requireContext(),
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().finish()
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


}


