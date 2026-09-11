package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.getOpenWithUris
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.capture.Document
import org.slf4j.LoggerFactory

@AndroidEntryPoint
open class SplashActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if (savedInstanceState == null && isIntentActionViewOrSend(intent)) {
            // For "open with" (file import) tests
            (applicationContext as ExampleApp).incrementIdlingResourceForOpenWith()

            startGiniBankSdk(intent)
        }
    }

    private fun startGiniBankSdk(intent: Intent) {
        configureGiniBank()
        if (configurationViewModel.configurationFlow.value.isOpenWithUriBasedApiEnabled) {
            // An Intent without Uris yields an empty list, which the SDK reports as an Error
            // through the same callback as every other failure
            val uris = getOpenWithUris(intent)
            Toast.makeText(
                this,
                getString(R.string.open_with_uri_based_api_toast),
                Toast.LENGTH_SHORT
            ).show()
            LOG.info("Open with: using Uri-based API (createDocumentForImportedFiles)")
            GiniBank.createDocumentForImportedFiles(
                uris = uris,
                context = this,
                callback = ::handleDocumentCreationResult
            )
        } else {
            LOG.info("Open with: using Intent-based API")
            GiniBank.createDocumentForImportedFiles(
                intent = intent,
                context = this,
                callback = ::handleDocumentCreationResult
            )
        }
    }

    private fun handleDocumentCreationResult(
        documentCreationResult: GiniBank.CreateDocumentFromImportedFileResult
    ) {
        when (documentCreationResult) {
            GiniBank.CreateDocumentFromImportedFileResult.Cancelled -> abortOpenWith("Open with cancelled")
            is GiniBank.CreateDocumentFromImportedFileResult.Error -> abortOpenWith("Open with failed with error ${documentCreationResult.error}")
            is GiniBank.CreateDocumentFromImportedFileResult.Success -> documentCreationResult.document?.let {
                startMainActivity(it)
            } ?: run {
                abortOpenWith("Open with failed")
            }
        }
    }

    /**
     * Terminal exit for an "open with" launch that did not produce a document. Shows the reason,
     * releases the idling resource incremented in [onCreate] (normally released by the
     * [ExtractionsActivity], which is never reached in this case) and closes the splash screen.
     */
    private fun abortOpenWith(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
        // For "open with" (file import) tests
        (applicationContext as ExampleApp).decrementIdlingResourceForOpenWith()
        finish()
    }

    private fun configureGiniBank() {
        configurationViewModel.clearGiniCaptureNetworkInstances()
        configurationViewModel.configureGiniBank(this)
    }

    open fun startMainActivity(document: Document) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_IN_OPEN_WITH_DOCUMENT, document)
        })
        finish()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SplashActivity::class.java)
    }
}