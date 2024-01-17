package net.gini.android.bank.sdk.capture

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItemsValidator

/**
 * Entry point for Screen API. It exists for the purpose of communication between Capture SDK's Screen API and Return Assistant.
 */
internal class CaptureFlowActivity : AppCompatActivity(), CaptureFlowImportContract.Contract {

    private val cameraLauncher = registerForActivityResult(CameraContract(), ::onCameraResult)
    private val cameraImportLauncher = registerForActivityResult(CaptureImportContract(), ::onCameraResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleInput()
        }
    }

    private fun handleInput() {
        when (val input = intent.getCaptureImportInput()) {
            is CaptureImportInput.Error -> setErrorResult(
                CaptureResult.Error(
                    ResultError.FileImport(
                        input.error,
                        input.message
                    )
                )
            )
            is CaptureImportInput.Forward -> cameraImportLauncher.launch(input.intent)
            CaptureImportInput.Default -> cameraLauncher.launch(Unit)
        }
    }

    private fun onCameraResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> setSuccessfulResult(result)
            CaptureResult.Empty -> setEmptyResult()
            is CaptureResult.Error -> setErrorResult(result)
            CaptureResult.EnterManually -> setEnterManuallyResult()
            CaptureResult.Cancel -> finish()

        }
    }

    private fun onDigitalInvoiceResult(result: CaptureResult) {
        when (result) {
            is CaptureResult.Success -> setSuccessfulResult(result)
            CaptureResult.Empty -> setEmptyResult()
            is CaptureResult.Error -> setErrorResult(result)
            CaptureResult.Cancel -> finish()
            CaptureResult.EnterManually -> setEnterManuallyResult()
        }
    }

    private fun setSuccessfulResult(result: CaptureResult.Success) {
        setResult(RESULT_OK, result.toIntent())
        finish()
    }

    private fun setErrorResult(errorResult: CaptureResult.Error) {
        when (errorResult.value) {
            is ResultError.FileImport -> setResult(
                CameraActivity.RESULT_ERROR,
                Intent().setImportResultError(errorResult.value.code, errorResult.value.message)
            )
            is ResultError.Capture -> setResult(CameraActivity.RESULT_ERROR, Intent().setCaptureResultError(errorResult.value.giniCaptureError))
        }
        finish()
    }

    private fun setEmptyResult() {
        setResult(RESULT_OK)
        finish()
    }

    private fun setEnterManuallyResult() {
        setResult(CameraActivity.RESULT_ENTER_MANUALLY)
        finish()
    }
}