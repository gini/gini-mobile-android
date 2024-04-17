package net.gini.android.bank.sdk.capture

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import kotlinx.parcelize.Parcelize
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.capture.Document
import net.gini.android.capture.internal.util.FileImportValidator

/**
 * Entry point for Screen API. It exists for the purpose of communication between Capture SDK's Screen API and Return Assistant.
 */
internal class CaptureFlowActivity : AppCompatActivity(), CaptureFlowFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gbs_activity_capture_flow)
        if (savedInstanceState == null) {
            handleInput()
        } else {
            restoreFragmentListener()
        }
    }

    private fun handleInput() {
        when (val input = getCaptureImportInput(intent)) {
            is CaptureImportInput.Error -> setResultAndFinish(
                CaptureResult.Error(
                    ResultError.FileImport(
                        input.error,
                        input.message
                    )
                )
            )
            is CaptureImportInput.Forward -> initFragment(input.openWithDocument)
            CaptureImportInput.Default -> initFragment()
        }
    }

    private fun getCaptureImportInput(intent: Intent): CaptureImportInput =
        IntentCompat.getParcelableExtra(intent, EXTRA_IN_CAPTURE_IMPORT_INPUT, CaptureImportInput::class.java)
            ?: CaptureImportInput.Default

    private fun initFragment(document: Document? = null) {
        if (!isFragmentShown()) {
            val fragment = createFragment(document)
            showFragment(fragment)
        }
    }

    private fun showFragment(fragment: CaptureFlowFragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.gbs_fragment_container, fragment, CaptureFlowFragment::class.java.name)
            .commit()
    }

    private fun isFragmentShown(): Boolean {
        return supportFragmentManager.findFragmentByTag(CaptureFlowFragment::class.java.name) != null
    }

    private fun createFragment(openWithDocument: Document?): CaptureFlowFragment {
        val fragment = if (openWithDocument != null) {
            GiniBank.createCaptureFlowFragmentForDocument(openWithDocument)
        } else {
            GiniBank.createCaptureFlowFragment()
        }
        fragment.setListener(this)
        return fragment
    }

    private fun restoreFragmentListener() {
        val fragment =
            supportFragmentManager.findFragmentByTag(CaptureFlowFragment::class.java.name) as CaptureFlowFragment?
        fragment?.setListener(this)
    }

    override fun onFinishedWithResult(result: CaptureResult) {
        setResultAndFinish(result)
    }

    private fun setResultAndFinish(result: CaptureResult) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_OUT_RESULT, result)
        })
        finish()
    }

    internal companion object {
        const val EXTRA_IN_CAPTURE_IMPORT_INPUT = "GBS_EXTRA_IN_CAPTURE_IMPORT_INPUT"
        const val EXTRA_OUT_RESULT = "GBS_EXTRA_OUT_RESULT"
    }

}

/**
 * Input used when a document was shared from another app. It will be created internally.
 */
@Parcelize
sealed class CaptureImportInput: Parcelable {
    data class Forward(val openWithDocument: Document) : CaptureImportInput()
    data class Error(val error: FileImportValidator.Error? = null, val message: String? = null) : CaptureImportInput()
    object Default : CaptureImportInput()
}