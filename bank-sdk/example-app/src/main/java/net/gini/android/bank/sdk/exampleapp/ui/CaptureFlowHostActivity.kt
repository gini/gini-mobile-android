package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentContainerView
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.capture.util.SharedPreferenceHelper

@AndroidEntryPoint
class CaptureFlowHostActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applied before super.onCreate() so it takes effect without recreating the activity.
        // This forces the theme for the SDK flow only; the rest of the example app is unaffected,
        // simulating a client that pins the SDK's appearance (see ConfigurationActivity QA toggles).
        applyForcedSdkTheme()
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

    /**
     * QA-only: applies the force-theme preference set in [ConfigurationActivity] as a *local*
     * night mode, so only the SDK capture flow is forced into dark/light while the host app keeps
     * following the system. No preference (or an unknown value) leaves it system-driven.
     */
    private fun applyForcedSdkTheme() {
        delegate.localNightMode = when (SharedPreferenceHelper.getString(FORCE_SDK_THEME_KEY, this)) {
            FORCE_SDK_THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            FORCE_SDK_THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    companion object {
        // QA force-theme preference shared with ConfigurationActivity.
        const val FORCE_SDK_THEME_KEY = "force_sdk_theme_key"
        const val FORCE_SDK_THEME_DARK = "dark"
        const val FORCE_SDK_THEME_LIGHT = "light"

        fun newIntent(context: Context) = Intent(context, CaptureFlowHostActivity::class.java)
    }

}