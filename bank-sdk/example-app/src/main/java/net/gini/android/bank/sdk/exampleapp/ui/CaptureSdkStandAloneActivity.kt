package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.di.BankSdkIsolatedKoinContext
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.ui.util.CaptureResultListener

@AndroidEntryPoint
class CaptureSdkStandAloneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        BankSdkIsolatedKoinContext.init(this)
        super.onCreate(savedInstanceState)
        // same view is required for both Bank SDK and Capture SDK, no need to create a separate xml
        setContentView(R.layout.activity_capture_flow_host)
        if (savedInstanceState == null) {
            if (intent != null && isIntentActionViewOrSend(intent)) {
                ClientCaptureSDKFragment().startCaptureSDKForIntent(
                    this,
                    intent,
                    CaptureResultListener(this)
                )

            } else {
                // start simple capture SDK
                val giniCaptureFragment = ClientCaptureSDKFragment()
                this.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_host, giniCaptureFragment, "fragment_host")
                    .addToBackStack(null).commit()
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CaptureSdkStandAloneActivity::class.java)
    }
}