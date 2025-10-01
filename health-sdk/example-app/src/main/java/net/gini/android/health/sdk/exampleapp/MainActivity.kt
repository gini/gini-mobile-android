package net.gini.android.health.sdk.exampleapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import net.gini.android.health.sdk.exampleapp.configuration.ConfigurationFragment
import net.gini.android.health.sdk.exampleapp.invoices.ui.AppCompatThemeInvoicesActivity
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesActivity
import net.gini.android.health.sdk.exampleapp.orders.OrdersActivity
import net.gini.android.health.sdk.exampleapp.upload.UploadActivity
import net.gini.android.health.sdk.exampleapp.util.SharedPreferencesUtil
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()

    private val useTestDocument = false
    private val testDocumentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLogging()

        setContent {
            MainScreen(
                viewModel = viewModel,
                useTestDocument = useTestDocument,
                testDocumentId = testDocumentId,
                onOpenConfiguration = ::openConfigurationScreen,
                onStartUpload = { uris ->
                    startActivity(
                        UploadActivity.getStartIntent(this, uris).apply {
                            viewModel.getPaymentFlowConfiguration()?.let {
                                putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                            }
                        }
                    )
                },
                onOpenInvoicesM3 = {
                    startActivity(Intent(this, InvoicesActivity::class.java).apply {
                        viewModel.getPaymentFlowConfiguration()?.let {
                            putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                        }
                    })
                },
                onOpenInvoicesAppCompat = {
                    startActivity(Intent(this, AppCompatThemeInvoicesActivity::class.java).apply {
                        viewModel.getPaymentFlowConfiguration()?.let {
                            putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                        }
                    })
                },
                onOpenOrders = {
                    startActivity(Intent(this, OrdersActivity::class.java).apply {
                        viewModel.getPaymentFlowConfiguration()?.let {
                            putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                        }
                    })
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        SharedPreferencesUtil.getStringFromSharedPreferences(
            SharedPreferencesUtil.PAYMENTREQUEST_KEY,
            this
        )?.let { viewModel.getPaymentRequest(it) }
    }

    private fun configureLogging() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.reset()
        val layoutEncoder = PatternLayoutEncoder().apply {
            context = lc
            pattern = "%-5level %file:%line [%thread] - %msg%n"
            start()
        }
        val logcatAppender = LogcatAppender().apply {
            context = lc
            encoder = layoutEncoder
            start()
        }
        val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(logcatAppender)
    }

    private fun openConfigurationScreen() {
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, ConfigurationFragment.newInstance(), ConfigurationFragment::class.java.name)
            .addToBackStack(ConfigurationFragment::class.java.name)
            .commit()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
        const val PAYMENT_FLOW_CONFIGURATION = "payment_flow_config"
    }
}
