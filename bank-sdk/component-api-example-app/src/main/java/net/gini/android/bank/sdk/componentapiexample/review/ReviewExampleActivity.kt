package net.gini.android.bank.sdk.componentapiexample.review

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
import net.gini.android.capture.review.ReviewFragmentCompat
import net.gini.android.capture.review.ReviewFragmentInterface
import net.gini.android.capture.review.ReviewFragmentListener
import net.gini.android.bank.sdk.componentapiexample.R
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisContract
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisInput

class ReviewExampleActivity : AppCompatActivity(), ReviewFragmentListener {

    private var reviewFragmentInterface: ReviewFragmentInterface? = null
    private val analysisLauncher = registerForActivityResult(AnalysisContract()) { ok ->
        if (ok) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        setUpActionBar()
        setTitles()

        if (savedInstanceState == null) {
            createReviewFragment()
            showReviewFragment()
        } else {
            retrieveReviewFragment()
        }
    }

    override fun onError(error: GiniCaptureError) {
        Toast.makeText(this, getString(R.string.gini_capture_error, error.errorCode, error.message), Toast.LENGTH_LONG).show()
    }

    override fun onProceedToAnalysisScreen(document: Document, errorMessage: String?) {
        analysisLauncher.launch(AnalysisInput(document, errorMessage))
    }

    private fun createReviewFragment(): ReviewFragmentInterface? {
        with(ReviewContract) {
            return intent.getDocument().let { document ->
                ReviewFragmentCompat.createInstance(document)?.also {
                    reviewFragmentInterface = it
                }
            }
        }
    }

    private fun showReviewFragment() {
        reviewFragmentInterface?.let {
            supportFragmentManager.commit {
                replace(R.id.review_screen_container, it as Fragment)
            }
        }
    }

    private fun retrieveReviewFragment(): ReviewFragmentInterface? =
        (supportFragmentManager.findFragmentById(R.id.review_screen_container) as ReviewFragmentCompat?).also {
            reviewFragmentInterface = it
        }

    private fun setTitles() {
        supportActionBar?.run {
            setTitle(R.string.review_screen_title)
            subtitle = getString(R.string.review_screen_subtitle)
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
    }
}

class ReviewContract : ActivityResultContract<Document, Boolean>() {

    override fun createIntent(context: Context, document: Document): Intent =
        Intent(context, ReviewExampleActivity::class.java).apply {
            putExtra(EXTRA_IN_DOCUMENT, document)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }

    companion object : Contract {
        const val EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT"
    }

    private interface Contract {
        fun Intent.getDocument(): Document {
            val document = getParcelableExtra<Document>(EXTRA_IN_DOCUMENT)
            check(document != null) { "ReviewContract requires Document as Input" }
            return document
        }
    }
}