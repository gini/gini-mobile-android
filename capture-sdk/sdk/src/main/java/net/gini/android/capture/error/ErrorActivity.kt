package net.gini.android.capture.error

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import net.gini.android.capture.Document
import net.gini.android.capture.R
import net.gini.android.capture.camera.CameraActivity.RESULT_ENTER_MANUALLY
import net.gini.android.capture.internal.util.ActivityHelper
import net.gini.android.capture.ImageRetakeOptionsListener
import net.gini.android.capture.camera.CameraActivity.RESULT_CAMERA_SCREEN
import net.gini.android.capture.network.ErrorType
import net.gini.android.capture.noresults.NoResultsActivity

class ErrorActivity : AppCompatActivity(),
    ImageRetakeOptionsListener {

    private var mDocument: Document? = null
    private var mErrorType: ErrorType? = null
    private var mCustomError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gc_activity_error)
        setTitle("")
        readExtras()
        val supportActionBar = supportActionBar
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true)
            supportActionBar.setDisplayShowHomeEnabled(true)
        }
        if (savedInstanceState == null) {
            initFragment()
        }
        handleOnBackPressed()
    }

    override fun onBackToCameraPressed() {
        setResult(RESULT_CAMERA_SCREEN)
        finish()
    }

    override fun onEnterManuallyPressed() {
        setResult(RESULT_ENTER_MANUALLY)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initFragment() {
        val errorFragment = ErrorFragmentCompat.createInstance(mErrorType, mDocument, mCustomError)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.gc_fragment_error, errorFragment)
            .commit()
    }

    private fun readExtras() {
        val extras = intent.extras
        if (extras != null) {
            mDocument = extras.getParcelable(NoResultsActivity.EXTRA_IN_DOCUMENT)
            mErrorType = extras.getSerializable(EXTRA_IN_ERROR) as? ErrorType
            mCustomError = extras.getString(EXTRA_ERROR_STRING)
        }
    }

    private fun handleOnBackPressed() {
        ActivityHelper.interceptOnBackPressed(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val noResultsIntent = Intent()
                noResultsIntent.putExtra(NoResultsActivity.NO_RESULT_CANCEL_KEY, true)
                setResult(RESULT_CANCELED, noResultsIntent)
                finish()
            }
        })
    }

    companion object {
        /**
         * Internal use only.
         *
         * @suppress
         */
        const val ERROR_REQUEST = 999

        const val ERROR_SCREEN_REQUEST = 111

        const val EXTRA_IN_ERROR = "GC_EXTRA_IN_ERROR"

        const val EXTRA_ERROR_STRING = "GC_EXTRA_IN_ERROR"

    }
}
