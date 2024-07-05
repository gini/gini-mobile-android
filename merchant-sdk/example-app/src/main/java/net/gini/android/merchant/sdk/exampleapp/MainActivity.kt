package net.gini.android.merchant.sdk.exampleapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import net.gini.android.merchant.sdk.exampleapp.configuration.ConfigurationFragment
import net.gini.android.merchant.sdk.exampleapp.databinding.ActivityMainBinding
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.InvoicesActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.invoicesScreen.setOnClickListener {
            startActivity(Intent(this, InvoicesActivity::class.java).apply {
                viewModel.getFlowConfiguration()?.let {
                    putExtra(FLOW_CONFIGURATION, it)
                }
            })
        }

        with(binding.giniMerchantVersion) {
            text = "${getString(R.string.gini_merchant_version)} ${net.gini.android.merchant.sdk.BuildConfig.VERSION_NAME}"
            setOnClickListener {
                openConfigurationScreen()
            }
        }

        configureLogging()
    }

    private fun openConfigurationScreen() {
        supportFragmentManager.beginTransaction()
            .add(binding.configurationContainer.id, ConfigurationFragment.newInstance(), ConfigurationFragment::class.java.name)
            .addToBackStack(ConfigurationFragment::class.java.name)
            .commit()
    }

    private fun configureLogging() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.reset()
        val layoutEncoder = PatternLayoutEncoder()
        layoutEncoder.context = lc
        layoutEncoder.pattern = "%-5level %file:%line [%thread] - %msg%n"
        layoutEncoder.start()
        val logcatAppender = LogcatAppender()
        logcatAppender.context = lc
        logcatAppender.encoder = layoutEncoder
        logcatAppender.start()
        val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(logcatAppender)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
        val FLOW_CONFIGURATION = "flow_configuration"
    }
}
