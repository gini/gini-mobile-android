package net.gini.android.bank.sdk.componentapiexample.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.review.multipage.MultiPageReviewFragment
import net.gini.android.capture.review.multipage.MultiPageReviewFragmentListener
import net.gini.android.bank.sdk.componentapiexample.R
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisContract
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisInput

class MultiPageReviewExampleActivity : AppCompatActivity(), MultiPageReviewFragmentListener {

    private var multiPageReviewFragment: MultiPageReviewFragment? = null
    private val analysisLauncher = registerForActivityResult(AnalysisContract()) { ok ->
        if (ok) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_page_review)
        setUpActionBar()
        setTitles()
        if (savedInstanceState == null) {
            createMultiPageReviewFragment()
            showMultiPageReviewFragment()
        } else {
            retrieveMultiPageReviewFragment()
        }
    }

    override fun onProceedToAnalysisScreen(document: GiniCaptureMultiPageDocument<*, *>) {
        analysisLauncher.launch(AnalysisInput(document))
    }

    override fun onReturnToCameraScreen() {
        finish()
    }

    override fun onImportedDocumentReviewCancelled() {
        finish()
    }

    override fun onError(error: GiniCaptureError) {
        Log.e("GiniCapture", error.message)
        finish()
    }

    private fun createMultiPageReviewFragment() {
        multiPageReviewFragment = MultiPageReviewFragment.createInstance()
    }

    private fun showMultiPageReviewFragment() {
        multiPageReviewFragment?.let { fragment ->
            supportFragmentManager.commit {
                replace(R.id.multi_page_review_screen_container, fragment)
            }
        }
    }

    private fun retrieveMultiPageReviewFragment() {
        multiPageReviewFragment = supportFragmentManager.findFragmentById(R.id.multi_page_review_screen_container) as MultiPageReviewFragment?
    }

    private fun setUpActionBar() {
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
    }

    private fun setTitles() {
        supportActionBar?.run {
            setTitle(R.string.multi_page_review_screen_title)
            subtitle = getString(R.string.multi_page_review_screen_subtitle)
        }
    }
}

class MultiPageReviewContract : ActivityResultContract<Unit, Boolean>() {

    override fun createIntent(context: Context, document: Unit): Intent =
        Intent(context, MultiPageReviewExampleActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}