package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.capture.Document

@AndroidEntryPoint
open class SplashActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if (savedInstanceState == null && intent != null) {
            if (isIntentActionViewOrSend(intent)) {
                startGiniBankSdk(intent)
            }
        }
    }

    private fun startGiniBankSdk(intent: Intent) {
        configureGiniBank()
        GiniBank.createDocumentForImportedFiles(
            intent = intent,
            context = this,
            callback = { documentCreationResult ->
                when (documentCreationResult) {
                    GiniBank.CreateDocumentFromImportedFileResult.Cancelled -> showErrorToast("Open with cancelled")
                    is GiniBank.CreateDocumentFromImportedFileResult.Error -> showErrorToast("Open with failed with error ${documentCreationResult.error}")
                    is GiniBank.CreateDocumentFromImportedFileResult.Success -> documentCreationResult.document?.let {
                        startMainActivity(it)
                    } ?: run {
                        showErrorToast("Open with failed")
                    }
                }
            }
        )
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
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
}