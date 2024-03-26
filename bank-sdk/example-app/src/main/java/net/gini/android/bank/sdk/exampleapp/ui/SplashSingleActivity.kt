package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil
import net.gini.android.capture.Document


class SplashSingleActivity : SplashActivity() {
    override fun startMainActivity(document: Document) {
        startActivity(CaptureFlowHostActivity.newIntent(this, Intent().apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ExampleUtil.DOCUMENT, document)
        }))
        finish()
    }
}