package net.gini.pay.appcomponentapi.noresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import net.gini.android.capture.Document
import net.gini.android.capture.noresults.NoResultsFragmentCompat
import net.gini.android.capture.noresults.NoResultsFragmentListener
import net.gini.pay.appcomponentapi.R
import net.gini.pay.appcomponentapi.camera.CameraExampleActivity

class NoResultsExampleActivity : AppCompatActivity(), NoResultsFragmentListener {

    private var noResultsFragment: NoResultsFragmentCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_results)
        setUpActionBar()
        setTitles()

        if (savedInstanceState == null) {
            showNoResultsFragment()
        } else {
            retainNoResultsFragment()
        }
    }

    override fun onBackToCameraPressed() {
        startActivity(Intent(this, CameraExampleActivity::class.java))
        finish()
    }

    private fun retainNoResultsFragment() {
        noResultsFragment = supportFragmentManager.findFragmentById(R.id.no_results_screen_container) as NoResultsFragmentCompat
    }

    private fun showNoResultsFragment() {
        intent.getParcelableExtra<Document>(EXTRA_IN_DOCUMENT)?.let { document ->
            supportFragmentManager.commit {
                replace(R.id.no_results_screen_container, NoResultsFragmentCompat.createInstance(document).also {
                    noResultsFragment = it
                })
            }
        }
    }

    private fun setTitles() {
        supportActionBar?.run {
            title = getString(R.string.no_results_screen_title)
            subtitle = getString(R.string.no_results_screen_subtitle)
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
    }

    companion object {
        private const val EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT"

        fun getStartIntent(context: Context?, document: Document?): Intent =
            Intent(context, NoResultsExampleActivity::class.java).apply {
                putExtra(EXTRA_IN_DOCUMENT, document)
            }
    }
}