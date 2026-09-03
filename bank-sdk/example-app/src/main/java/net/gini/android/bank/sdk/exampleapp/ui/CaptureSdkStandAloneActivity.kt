package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.ExampleUtil.isIntentActionViewOrSend
import net.gini.android.bank.sdk.exampleapp.ui.util.CaptureResultListener
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureFragmentListener

@AndroidEntryPoint
class CaptureSdkStandAloneActivity : AppCompatActivity() {

    private var listener: CaptureResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        listener = CaptureResultListener(this)
        supportFragmentManager.fragmentFactory =
            ClientCaptureSDKFragmentFactory(listener!!, null)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stand_alone_capture_flow)
        if (savedInstanceState == null) {
            if (intent != null && isIntentActionViewOrSend(intent)) {
                ClientCaptureSDKFragment().startCaptureSDKForIntent(
                    this,
                    intent,
                    listener!!
                )
            } else {
                val clientCaptureSDK = ClientCaptureSDKFragment()
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_host,
                        clientCaptureSDK,
                        ClientCaptureSDKFragment::class.java.name
                    )
                    .addToBackStack(null)
                    .commit()
                clientCaptureSDK.setListener(listener = listener!!)
            }
        } else {
            restoreFragmentListener()
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CaptureSdkStandAloneActivity::class.java)
    }

    private fun restoreFragmentListener() {
        val fragment =
            supportFragmentManager.findFragmentByTag(
                ClientCaptureSDKFragment::class.java.name
            ) as? ClientCaptureSDKFragment?
        listener?.let { fragment?.setListener(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener = null
    }
}

class ClientCaptureSDKFragmentFactory(
    private val giniCaptureFragmentListener: GiniCaptureFragmentListener,
    private var openWithDocument: Document? = null
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            GiniCaptureFragment::class.java.name -> GiniCaptureFragment.createInstance(
                openWithDocument
            ) { openWithDocument = null }
                .apply {
                    setListener(
                        giniCaptureFragmentListener
                    )
                }

            else -> super.instantiate(classLoader, className)
        }
    }
}
