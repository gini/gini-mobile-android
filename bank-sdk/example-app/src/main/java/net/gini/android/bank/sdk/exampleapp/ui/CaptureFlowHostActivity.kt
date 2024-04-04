package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend

@AndroidEntryPoint
class CaptureFlowHostActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_flow_host)

        if (savedInstanceState == null) {
            if (intent != null && isIntentActionViewOrSend(intent)) {
                startBankSdkForOpenWith(intent)
            } else {
                startBankSdk()
            }
        }
    }

    private fun startBankSdk() {
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientBankSDKFragment>()
            .checkCameraPermissionAndStartBankSdk()
    }

    private fun startBankSdkForOpenWith(openWithIntent: Intent) {
        configureGiniBank()
        findViewById<FragmentContainerView>(R.id.fragment_host).getFragment<ClientBankSDKFragment>()
            .startBankSdkForIntent(openWithIntent)
    }

    private fun configureGiniBank() {
        configurationViewModel.clearGiniCaptureNetworkInstances()
        configurationViewModel.configureGiniBank(this)
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CaptureFlowHostActivity::class.java)
    }

}