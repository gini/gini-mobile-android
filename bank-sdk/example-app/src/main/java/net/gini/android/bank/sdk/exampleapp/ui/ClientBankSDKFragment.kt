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
import net.gini.android.bank.sdk.capture.CaptureFlowFragment
import net.gini.android.bank.sdk.capture.CaptureFlowFragmentListener
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.PermissionHandler
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ProductTag

class ClientBankSDKFragment :
    Fragment(R.layout.fragment_client),
    CaptureFlowFragmentListener {

    private lateinit var permissionHandler: PermissionHandler

    private var wasCameraPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val captureFlowFragment =
                requireActivity().supportFragmentManager.findFragmentByTag("fragment_host") as? CaptureFlowFragment
            captureFlowFragment?.setListener(this)
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    fun checkCameraPermissionAndStartBankSdk() {
        permissionHandler = PermissionHandler(requireActivity())
        lifecycleScope.launch {
            if (permissionHandler.grantPermission(Manifest.permission.CAMERA)) {
                // Bank SDK is configured in the MainActivity
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


    private fun startBankSDK() {
        val captureFlowFragment = GiniBank.createCaptureFlowFragment()
        captureFlowFragment.setListener(this)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_host, captureFlowFragment, "fragment_host")
            .addToBackStack(null)
            .commit()
    }

    fun startBankSdkForIntent(openWithIntent: Intent) {
        // Bank SDK is configured in the MainActivity
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

    override fun onFinishedWithResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> {
                startActivity(
                    ExtractionsActivity.getStartIntent(
                        requireContext(),
                        result.specificExtractions,
                        result.compoundExtractions,
                        GiniCapture.getInstance().productTag == ProductTag.CxExtractions,
                    )
                )
                activity?.finish()
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
                activity?.finish()
            }

            CaptureResult.Empty -> {
                activity?.finish()
            }

            CaptureResult.Cancel -> {
                activity?.finish()
            }

            CaptureResult.EnterManually -> {
                Toast.makeText(
                    requireContext(),
                    "Scan exited for manual enter mode",
                    Toast.LENGTH_SHORT
                ).show()
                activity?.finish()
            }
        }
    }


}


