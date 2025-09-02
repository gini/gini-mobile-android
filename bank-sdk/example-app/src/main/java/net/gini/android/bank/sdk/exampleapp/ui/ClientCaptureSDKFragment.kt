package net.gini.android.bank.sdk.exampleapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
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
    private var captureFragmentListener: GiniCaptureFragmentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(requireContext(), "ClientCaptureSDKFragment", Toast.LENGTH_SHORT).show()
        if (savedInstanceState == null) {
            checkCameraPermission()
        } else {
            val giniCaptureFragment =
                requireActivity().supportFragmentManager.findFragmentByTag("fragment_host")
                        as?
                        GiniCaptureFragment
            giniCaptureFragment?.setListener(this)
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    private fun checkCameraPermission() {
        permissionHandler = PermissionHandler(requireActivity())
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                configureCaptureSDK(requireContext())
                startCaptureSDK()
            }
        }
    }

    private fun configureCaptureSDK(context: Context) {
        val clientId = context.getString(R.string.gini_api_client_id)
        val clientSecret = context.getString(R.string.gini_api_client_secret)
        val documentMetadata = DocumentMetadata()
        documentMetadata.setBranchId("GCSExampleAndroid")
        documentMetadata.add("AppFlow", "ScreenAPI")

        val networkService = GiniCaptureDefaultNetworkService
            .builder(context)
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

    fun setListener(listener: GiniCaptureFragmentListener) {
        this.captureFragmentListener = listener
    }

    fun startCaptureSDKForIntent(
        context: AppCompatActivity,
        openWithIntent: Intent,
        listener: GiniCaptureFragmentListener
    ) {
        configureCaptureSDK(context)
        mFileImportCancellationToken = GiniCapture.getInstance()
            .createGiniCaptureFragmentForIntent(
                context,
                openWithIntent,
                object : GiniCapture.CreateGiniCaptureFragmentForIntentCallback {
                    override fun callback(
                        result: GiniCapture.CreateGiniCaptureFragmentForIntentResult?
                    ) {
                        when (result) {
                            is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Success -> {
                                // Opening the file(s) from the intent and creating the
                                // GiniCaptureFragment finished

                                // Set the listener to receive the Gini Bank SDK's results
                                result.fragment.setListener(listener)
                                // Show the CaptureFlowFragment for example via
                                // the fragment manager:
                                context.supportFragmentManager.beginTransaction()
                                    .replace(
                                        R.id.fragment_host,
                                        result.fragment,
                                        "GiniCaptureFragment"
                                    )
                                    .addToBackStack(null)
                                    .commit()
                            }

                            is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Error -> {
                                // Something went wrong when opening the file(s) from the intent or
                                // uploading the document
                                Toast.makeText(
                                    context,
                                    "Error, Exiting",
                                    Toast.LENGTH_SHORT
                                ).show()
                                context.finish()
                            }

                            is GiniCapture.CreateGiniCaptureFragmentForIntentResult.Cancelled -> {
                                Toast.makeText(
                                    context,
                                    "Cancelled, Exiting",
                                    Toast.LENGTH_SHORT
                                ).show()
                                context.finish()
                            }
                        }
                    }

                })

    }

    override fun onFinishedWithResult(result: CaptureSDKResult) {
        finishWithResult(result)
    }

    private fun finishWithResult(result: CaptureSDKResult) {
        captureFragmentListener?.onFinishedWithResult(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        captureFragmentListener = null
        if (mFileImportCancellationToken != null) {
            mFileImportCancellationToken!!.cancel()
            mFileImportCancellationToken = null
        }
    }
}
