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
import net.gini.android.capture.noresults.NoResultsActivity

class ErrorActivity : AppCompatActivity(),
    ImageRetakeOptionsListener {

    private var mDocument: Document? = null

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
        val errorFragment = ErrorFragmentCompat.createInstance(mDocument)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.gc_fragment_error, errorFragment)
            .commit()
    }

    private fun readExtras() {
        val extras = intent.extras
        if (extras != null) {
            mDocument = extras.getParcelable(NoResultsActivity.EXTRA_IN_DOCUMENT)
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
    }
}
