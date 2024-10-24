package net.gini.android.health.sdk.exampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import com.google.android.material.tabs.TabLayoutMediator
import net.gini.android.health.sdk.exampleapp.configuration.ConfigurationFragment
import net.gini.android.health.sdk.exampleapp.databinding.ActivityMainBinding
import net.gini.android.health.sdk.exampleapp.invoices.ui.AppCompatThemeInvoicesActivity
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesActivity
import net.gini.android.health.sdk.exampleapp.pager.PagerAdapter
import net.gini.android.health.sdk.exampleapp.review.ReviewActivity
import net.gini.android.health.sdk.exampleapp.upload.UploadActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture(), ::photoResult)
    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), ::importResult)
    private lateinit var binding: ActivityMainBinding

    private val useTestDocument = false
    private val testDocumentId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.takePhoto.setOnClickListener {
            takePhoto()
        }

        binding.importFile.setOnClickListener {
            if (useTestDocument) {
                viewModel.setDocumentForReview(testDocumentId)
                startActivity(ReviewActivity.getStartIntent(this, paymentComponentConfiguration = viewModel.getPaymentComponentConfiguration()))
            } else {
                importFile()
            }
        }

        binding.pager.adapter = PagerAdapter().apply {
            lifecycleScope.launchWhenStarted {
                viewModel.pages.collect { pages ->
                    submitList(pages)
                }
            }
        }

        TabLayoutMediator(binding.indicator, binding.pager) { _, _ -> }.attach()

        binding.upload.setOnClickListener {
            startActivity(
                UploadActivity.getStartIntent(
                    this@MainActivity,
                    viewModel.pages.value.map { it.uri },
                    viewModel.getPaymentComponentConfiguration(),
                    viewModel.getPaymentFlowConfiguration()
                )
            )
        }

        binding.invoicesScreen.setOnClickListener {
            startActivity(Intent(this, InvoicesActivity::class.java).apply {
                viewModel.getPaymentComponentConfiguration()?.let {
                    putExtra(PAYMENT_COMPONENT_CONFIG, it)
                }
                viewModel.getPaymentFlowConfiguration()?.let {
                    putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                }
            })
        }

        binding.appcompatThemeInvoicesScreen.setOnClickListener {
            startActivity(Intent(this, AppCompatThemeInvoicesActivity::class.java).apply {
                viewModel.getPaymentComponentConfiguration()?.let {
                    putExtra(PAYMENT_COMPONENT_CONFIG, it)
                }
                viewModel.getPaymentFlowConfiguration()?.let {
                    putExtra(PAYMENT_FLOW_CONFIGURATION, it)
                }
            })
        }

        with(binding.giniHealthVersion) {
            text = "${getString(R.string.gini_health_version)} ${net.gini.android.health.sdk.BuildConfig.VERSION_NAME}"
            setOnClickListener {
                openConfigurationScreen()
            }
        }

        configureLogging()
    }

    private fun importFile() {
        importLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    private fun takePhoto() {
        takePictureLauncher.launch(viewModel.getNextPageUri(this@MainActivity))
    }

    private fun photoResult(saved: Boolean) {
        if (saved) {
            viewModel.onPhotoSaved()
            binding.upload.isEnabled = true
        }
    }

    private fun importResult(uri: Uri?) {
        uri?.let {
            startActivity(UploadActivity.getStartIntent(this, listOf(it), viewModel.getPaymentComponentConfiguration(), viewModel.getPaymentFlowConfiguration()))
        } ?: run {
            Toast.makeText(this, "No document received", Toast.LENGTH_LONG).show()
        }
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

    private fun openConfigurationScreen() {
        supportFragmentManager.beginTransaction()
            .add(binding.configurationContainer.id, ConfigurationFragment.newInstance(), ConfigurationFragment::class.java.name)
            .addToBackStack(ConfigurationFragment::class.java.name)
            .commit()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
        val PAYMENT_COMPONENT_CONFIG = "payment_component_config"
        const val PAYMENT_FLOW_CONFIGURATION = "payment_flow_config"
    }
}
