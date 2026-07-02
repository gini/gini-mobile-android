package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.ui.color.ColorOverrideContextWrapper
import net.gini.android.bank.sdk.exampleapp.ui.color.SdkColorOverridePreference
import net.gini.android.bank.sdk.exampleapp.ui.color.SdkColorOverrides

@AndroidEntryPoint
class CaptureFlowHostActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    // When the "Override SDK colors" flag is on, install a Resources wrapper as the base context so
    // that every SDK color read (Compose theme via GiniTheme and legacy XML @color/gc_* references)
    // resolves to the distinct override palette. Read here because attachBaseContext runs before the
    // configuration intent extra is available.
    override fun attachBaseContext(newBase: Context) {
        if (SdkColorOverridePreference.isEnabled(newBase)) {
            val isNight = (newBase.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overrides = if (isNight) SdkColorOverrides.dark else SdkColorOverrides.light
            super.attachBaseContext(ColorOverrideContextWrapper(newBase, overrides))
        } else {
            super.attachBaseContext(newBase)
        }
    }

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
        // For "open with" (file import) tests
        (applicationContext as ExampleApp).incrementIdlingResourceForOpenWith()

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