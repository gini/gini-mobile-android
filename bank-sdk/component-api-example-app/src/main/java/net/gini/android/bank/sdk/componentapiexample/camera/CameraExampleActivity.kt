package net.gini.android.bank.sdk.componentapiexample.camera

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureCoordinator
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.ImportedFileValidationException
import net.gini.android.capture.camera.CameraFragmentCompat
import net.gini.android.capture.camera.CameraFragmentInterface
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.help.HelpActivity
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.onboarding.OnboardingFragmentCompat
import net.gini.android.capture.onboarding.OnboardingFragmentListener
import net.gini.android.capture.util.IntentHelper
import net.gini.android.bank.sdk.componentapiexample.R
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisContract
import net.gini.android.bank.sdk.componentapiexample.analysis.AnalysisInput
import net.gini.android.bank.sdk.componentapiexample.databinding.ActivityCameraBinding
import net.gini.android.bank.sdk.componentapiexample.extraction.ExtractionsActivity
import net.gini.android.bank.sdk.componentapiexample.review.MultiPageReviewContract
import net.gini.android.bank.sdk.componentapiexample.review.ReviewContract
import net.gini.android.bank.sdk.componentapiexample.util.hasLessThan5MB
import net.gini.android.bank.sdk.componentapiexample.util.isIntentActionViewOrSend
import net.gini.android.bank.sdk.GiniBank
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CameraExampleActivity : AppCompatActivity(), CameraFragmentListener, OnboardingFragmentListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var mGiniCaptureCoordinator: GiniCaptureCoordinator
    private var mCameraFragmentInterface: CameraFragmentInterface? = null
    private var menu: Menu? = null

    private val reviewLauncher = registerForActivityResult(ReviewContract()) { ok -> if (ok) finish() }
    private val multiPageReviewLauncher = registerForActivityResult(MultiPageReviewContract()) { ok -> if (ok) finish() }
    private val analysisLauncher = registerForActivityResult(AnalysisContract()) { ok -> if (ok) finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar(binding)
        setTitlesForCamera()

        setupGiniCaptureCoordinator()

        if (savedInstanceState == null) {
            if (isIntentActionViewOrSend(intent)) {
                startGiniCaptureSdkForImportedFile(intent)
            } else {
                showCamera()
            }
        } else {
            mCameraFragmentInterface = retrieveCameraFragment()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_camera, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.tips -> {
            showOnboarding(binding)
            true
        }
        R.id.help -> {
            showHelp()
            true
        }
        else -> false
    }

    override fun onBackPressed() {
        if (isOnboardingVisible()) {
            removeOnboarding()
        } else {
            super.onBackPressed()
        }
    }

    private fun isOnboardingVisible(): Boolean = supportFragmentManager.findFragmentById(R.id.onboarding_container) != null

    private fun removeOnboarding() {
        LOG.debug("Remove the Onboarding Screen")
        menu?.setGroupVisible(R.id.group, true)
        mCameraFragmentInterface?.showInterface()
        setTitlesForCamera()
        removeOnboardingFragment()
    }

    private fun removeOnboardingFragment() {
        supportFragmentManager.findFragmentById(R.id.onboarding_container)?.let { fragment ->
            supportFragmentManager.commit(allowStateLoss = true) {
                setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                remove(fragment)
            }
        }
    }

    private fun setupGiniCaptureCoordinator() {
        mGiniCaptureCoordinator = GiniCaptureCoordinator.createInstance(this)
        mGiniCaptureCoordinator.setListener { showOnboarding(binding) }
    }


    private fun startGiniCaptureSdkForImportedFile(intent: Intent) {
        GiniBank.createDocumentForImportedFiles(intent, this, object : AsyncCallback<Document, ImportedFileValidationException> {
            override fun onSuccess(result: Document) {
                if (result.isReviewable) {
                    launchMultiPageReviewScreen()
                } else {
                    launchAnalysisScreen(result)
                }
                finish()
            }

            override fun onError(exception: ImportedFileValidationException) {
                handleFileImportError(exception)
            }

            override fun onCancelled() {
            }
        })
    }

    private fun launchReviewScreen(document: Document) {
        reviewLauncher.launch(document)
    }

    private fun launchMultiPageReviewScreen() {
        multiPageReviewLauncher.launch(Unit)
    }

    private fun launchAnalysisScreen(document: Document) {
        analysisLauncher.launch(AnalysisInput(document))
    }

    private fun handleFileImportError(exception: ImportedFileValidationException) {
        AlertDialog.Builder(this)
            .setMessage(exception.validationError?.let { getString(it.textResource) } ?: exception.message)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun showCamera() {
        LOG.debug("Show the Camera Screen")
        mCameraFragmentInterface = createCameraFragment()
        showCameraFragment()
        mGiniCaptureCoordinator.onCameraStarted()
    }

    private fun createCameraFragment(): CameraFragmentInterface =
        CameraFragmentCompat.createInstance().also {
            mCameraFragmentInterface = it
        }


    private fun showCameraFragment() {
        (mCameraFragmentInterface as Fragment?)?.let { fragment ->
            supportFragmentManager.commit {
                replace(R.id.camera_container, fragment)
            }
        }
    }

    private fun retrieveCameraFragment(): CameraFragmentInterface =
        (supportFragmentManager.findFragmentById(R.id.camera_container) as CameraFragmentCompat).also {
            mCameraFragmentInterface = it
        }

    private fun showOnboarding(binding: ActivityCameraBinding) {
        LOG.debug("Show the Onboarding Screen")
        menu?.setGroupVisible(R.id.group, false)
        mCameraFragmentInterface?.hideInterface()
        setTitlesForOnboarding(binding.toolbar)
        showOnboardingFragment()
    }

    private fun showHelp() {
        startActivity(Intent(this, HelpActivity::class.java))
    }

    private fun showOnboardingFragment() {
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            replace(R.id.onboarding_container, OnboardingFragmentCompat())
        }
    }

    private fun setUpActionBar(binding: ActivityCameraBinding) {
        setSupportActionBar(binding.toolbar)
    }

    private fun setTitlesForCamera() {
        supportActionBar?.title = getString(R.string.camera_screen_title)
        supportActionBar?.subtitle = getString(R.string.camera_screen_subtitle)
    }

    private fun setTitlesForOnboarding(toolbar: Toolbar) {
        toolbar.title = ""
        toolbar.subtitle = ""
    }

    override fun onDocumentAvailable(document: Document) {
        if (document.isReviewable) {
            launchReviewScreen(document)
        } else {
            launchAnalysisScreen(document)
        }
    }

    override fun onProceedToMultiPageReviewScreen(multiPageDocument: GiniCaptureMultiPageDocument<*, *>) {
        launchMultiPageReviewScreen()
    }

    override fun onCheckImportedDocument(document: Document, callback: CameraFragmentListener.DocumentCheckResultCallback) {
        // We can apply custom checks here to an imported document and notify the Gini Capture SDK
        // about the result

        // As an example we allow only documents smaller than 5MB
        val customCheck = false
        if (customCheck) {
            // Use the Intent with which the document was imported to access its contents (document.getData() may be null)
            val intent = document.intent
            if (intent == null) {
                callback.documentRejected(getString(R.string.gc_document_import_error))
                return
            }
            val uri = IntentHelper.getUri(intent)
            if (uri == null) {
                callback.documentRejected(getString(R.string.gc_document_import_error))
                return
            }
            // IMPORTANT: always call one of the callback methods
            if (hasLessThan5MB(uri)) {
                callback.documentAccepted()
            } else {
                callback.documentRejected(getString(R.string.document_size_too_large))
            }
        } else {
            // IMPORTANT: always call one of the callback methods
            callback.documentAccepted()
        }
    }

    override fun onCloseOnboarding() {
        removeOnboarding()
    }

    override fun onError(error: GiniCaptureError) {
        LOG.error("Gini Capture SDK error: {} - {}", error.errorCode, error.message)
        Toast.makeText(this, getString(R.string.gini_capture_error, error.errorCode, error.message), Toast.LENGTH_LONG).show()
    }

    override fun onExtractionsAvailable(extractions: Map<String, GiniCaptureSpecificExtraction>) {
        startActivity(ExtractionsActivity.getStartIntent(this, extractions))
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CameraExampleActivity::class.java)
    }
}