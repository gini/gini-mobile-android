package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R

@AndroidEntryPoint
class CaptureFlowHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_flow_host)

        if (savedInstanceState == null) {
            if (isIntentActionViewOrSend(intent)) {
                startBankSDKForOpenWith(intent)
            }
        }
    }


    private fun startBankSDKForOpenWith(openWithIntent: Intent) {
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientGiniCaptureFragment>()
            .startBankSDKForIntent(openWithIntent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && isIntentActionViewOrSend(intent)) {
            startBankSDKForOpenWith(intent)
        }
    }

    private fun isIntentActionViewOrSend(intent: Intent): Boolean {
        val action = intent.action
        return Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action
    }

}