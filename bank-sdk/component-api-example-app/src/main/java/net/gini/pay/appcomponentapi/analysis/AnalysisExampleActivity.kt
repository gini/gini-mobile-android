package net.gini.pay.appcomponentapi.analysis

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.analysis.AnalysisActivity
import net.gini.android.capture.analysis.AnalysisFragmentCompat
import net.gini.android.capture.analysis.AnalysisFragmentInterface
import net.gini.android.capture.analysis.AnalysisFragmentListener
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.pay.appcomponentapi.R
import net.gini.pay.appcomponentapi.digitalinvoice.DigitalInvoiceExampleActivity
import net.gini.pay.appcomponentapi.extraction.ExtractionsActivity
import net.gini.pay.appcomponentapi.noresult.NoResultsExampleActivity
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItemsValidator
import org.slf4j.LoggerFactory

class AnalysisExampleActivity : AppCompatActivity(), AnalysisFragmentListener {

    private var analysisFragmentInterface: AnalysisFragmentInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)
        setUpActionBar()
        setTitles()

        if (savedInstanceState == null) {
            analysisFragmentInterface = createAnalysisFragment()
            showAnalysisFragment()
        } else {
            analysisFragmentInterface = retrieveAnalysisFragment()
        }
    }

    override fun onError(error: GiniCaptureError) {
        analysisFragmentInterface?.showError(getString(R.string.gini_capture_error, error.errorCode, error.message), Toast.LENGTH_LONG)
    }

    override fun onExtractionsAvailable(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        returnReasons: List<GiniCaptureReturnReason>
    ) {
        LOG.debug("Show extractions")
        try {
            LineItemsValidator.validate(compoundExtractions)
            startActivity(DigitalInvoiceExampleActivity.getStartIntent(this, extractions, compoundExtractions, returnReasons))

        } catch (notUsed: DigitalInvoiceException) {
            startActivity(ExtractionsActivity.getStartIntent(this, extractions, compoundExtractions))
        }
        setResult(RESULT_OK)
        finish()
    }

    override fun onProceedToNoExtractionsScreen(document: Document) {
        startActivity(NoResultsExampleActivity.getStartIntent(this, document))
        setResult(RESULT_OK)
        finish()
    }

    override fun onDefaultPDFAppAlertDialogCancelled() {
        finish()
    }

    private fun retrieveAnalysisFragment(): AnalysisFragmentInterface {
        return (supportFragmentManager.findFragmentById(R.id.analysis_screen_container) as AnalysisFragmentCompat).also {
            analysisFragmentInterface = it
        }
    }

    private fun showAnalysisFragment() {
        supportFragmentManager.commit {
            replace(R.id.analysis_screen_container, analysisFragmentInterface as Fragment)
        }
    }

    private fun createAnalysisFragment(): AnalysisFragmentInterface? {
        with(AnalysisContract) {
            val input = intent.getInput()
            return AnalysisFragmentCompat.createInstance(input.document, input.errorMessage).also {
                analysisFragmentInterface = it
            }
        }
    }

    private fun setTitles() {
        supportActionBar?.run {
            title = ""
            subtitle = getString(R.string.one_moment_please)
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar?)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(AnalysisActivity::class.java)
    }
}

data class AnalysisInput(
    val document: Document,
    val errorMessage: String? = null
)

class AnalysisContract : ActivityResultContract<AnalysisInput, Boolean>() {
    override fun createIntent(context: Context, input: AnalysisInput): Intent =
        Intent(context, AnalysisExampleActivity::class.java).apply {
            putExtra(EXTRA_IN_DOCUMENT, input.document)
            putExtra(EXTRA_IN_ERROR_MESSAGE, input.errorMessage)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

    companion object : Contract {
        private const val EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT"
        private const val EXTRA_IN_ERROR_MESSAGE = "EXTRA_IN_ERROR_MESSAGE"
    }

    private interface Contract {
        fun Intent.getInput(): AnalysisInput {
            val document = getParcelableExtra<Document>(EXTRA_IN_DOCUMENT)
            check(document != null) { "AnalysisContract requires Document as input" }
            return AnalysisInput(document, getStringExtra(EXTRA_IN_ERROR_MESSAGE))
        }
    }
}
