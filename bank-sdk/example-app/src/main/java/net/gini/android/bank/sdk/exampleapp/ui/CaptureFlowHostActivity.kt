package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.capture.Document

@AndroidEntryPoint
class CaptureFlowHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_flow_host)

        if (savedInstanceState == null) {
            val openWithIntent = IntentCompat.getParcelableExtra(intent, EXTRA_IN_OPEN_WITH_INTENT, Intent::class.java)
            val openDocument = IntentCompat.getParcelableExtra(intent, EXTRA_IN_OPEN_WITH_DOCUMENT, Document::class.java)
            if (openDocument != null) {
                startBankSDKForDocument(openDocument)
            } else if (openWithIntent != null) {
                intent.getParcelableExtra(EXTRA_IN_OPEN_WITH_DOCUMENT, Document::class.java)?.let {
                    startBankSDKForDocument(it)
                }
            }
        }
    }

    private fun startBankSDKForOpenWith(openWithIntent: Intent) {
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientBankSDKFragment>()
            .startBankSDKForIntent(openWithIntent)
    }


    private fun startCaptureSDKForOpenWith(openWithIntent: Intent) {
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientCaptureSDKFragment>()
            .startCaptureSDKForIntent(openWithIntent)
    }

    private fun startBankSDKForDocument(document: Document) {
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientBankSDKFragment>()
            .startBankSDKForDocument(document)
    }

    companion object {
        private const val EXTRA_IN_OPEN_WITH_INTENT = "EXTRA_IN_OPEN_WITH_INTENT"
        private const val EXTRA_IN_OPEN_WITH_DOCUMENT = "EXTRA_IN_OPEN_WITH_DOCUMENT"

        fun newIntent(context: Context, openWithIntent: Intent? = null) =
            Intent(context, CaptureFlowHostActivity::class.java).apply {
                openWithIntent?.let { putExtra(EXTRA_IN_OPEN_WITH_INTENT, it) }
            }

        fun newIntent(context: Context, openWithDocument: Document) =
            Intent(context, CaptureFlowHostActivity::class.java).apply {
                putExtra(EXTRA_IN_OPEN_WITH_DOCUMENT, openWithDocument)
            }
    }

}