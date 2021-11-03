package net.gini.pay.appcomponentapi.digitalinvoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import java.util.*
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.pay.appcomponentapi.R
import net.gini.pay.appcomponentapi.databinding.ActivityDigitalInvoiceBinding
import net.gini.pay.appcomponentapi.extraction.ExtractionsActivity
import net.gini.pay.appcomponentapi.util.toBundle
import net.gini.pay.appcomponentapi.util.toMap
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragmentListener
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.info.DigitalInvoiceInfoFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.info.DigitalInvoiceInfoFragmentListener
import net.gini.android.bank.sdk.capture.digitalinvoice.onboarding.DigitalInvoiceOnboardingFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.onboarding.DigitalInvoiceOnboardingFragmentListener
import org.slf4j.LoggerFactory

private const val TAG_ONBOARDING = "TAG_ONBOARDING"
private const val TAG_INFO = "TAG_INFO"

class DigitalInvoiceExampleActivity : AppCompatActivity(), DigitalInvoiceFragmentListener,
    DigitalInvoiceOnboardingFragmentListener, DigitalInvoiceInfoFragmentListener {

    private val lineItemDetailsLauncher =
        registerForActivityResult(LineItemDetailsContract()) { item ->
            item?.let { digitalInvoiceFragment.updateLineItem(item) }
        }
    private lateinit var digitalInvoiceFragment: DigitalInvoiceFragment
    private var extractions: Map<String, GiniCaptureSpecificExtraction> = emptyMap()
    private var compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap()
    private var returnReasons: List<GiniCaptureReturnReason> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDigitalInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitles()
        readExtras()
        if (savedInstanceState == null) {
            createDigitalInvoiceFragment()
            showDigitalInvoiceFragment()
        } else {
            retrieveDigitalInvoiceFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        if (item.itemId == net.gini.android.bank.sdk.R.id.help) {
            showInfo()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(net.gini.android.bank.sdk.R.menu.gbs_menu_digital_invoice, menu)
        return true
    }

    private fun createDigitalInvoiceFragment() {
        digitalInvoiceFragment =
            DigitalInvoiceFragment.createInstance(extractions, compoundExtractions, returnReasons)
    }

    private fun showDigitalInvoiceFragment() {
        supportFragmentManager.commit {
            replace(R.id.digital_invoice_screen_container, digitalInvoiceFragment)
        }
    }

    private fun retrieveDigitalInvoiceFragment() {
        digitalInvoiceFragment =
            supportFragmentManager.findFragmentById(R.id.digital_invoice_screen_container) as DigitalInvoiceFragment
    }

    private fun setTitles() {
        supportActionBar?.run {
            title = getString(R.string.digital_invoice_screen_title)
            subtitle = getString(R.string.digital_invoice_screen_subtitle)
        }
    }


    private fun readExtras() {
        extractions = intent.getBundleExtra(EXTRA_IN_EXTRACTIONS)?.toMap() ?: emptyMap()
        compoundExtractions =
            intent.getBundleExtra(EXTRA_IN_COMPOUND_EXTRACTIONS)?.toMap() ?: emptyMap()
        returnReasons = intent.getParcelableArrayListExtra(EXTRA_IN_RETURN_REASONS) ?: emptyList()
    }

    private fun showInfo() {
        if (supportFragmentManager.findFragmentByTag(TAG_INFO) != null) {
            return
        }
        supportFragmentManager.commit {
            val infoFragment = DigitalInvoiceInfoFragment.createInstance().apply {
                listener = this@DigitalInvoiceExampleActivity
            }
            add(R.id.digital_invoice_screen_container, infoFragment, TAG_INFO)
        }
    }

    override fun onCloseInfo() {
        (supportFragmentManager.findFragmentByTag(TAG_INFO) as? DigitalInvoiceInfoFragment)?.let { infoFragment ->
            infoFragment.listener = null
            supportFragmentManager.commit {
                remove(infoFragment)
            }
        }
    }

    override fun onEditLineItem(selectableLineItem: SelectableLineItem) {
        lineItemDetailsLauncher.launch(LineItemDetailsInput(selectableLineItem, returnReasons))
    }

    override fun onAddLineItem(selectableLineItem: SelectableLineItem) {
        lineItemDetailsLauncher.launch(LineItemDetailsInput(selectableLineItem, returnReasons))
    }

    override fun showOnboarding() {
        supportFragmentManager.commit {
            val onboardingFragment = DigitalInvoiceOnboardingFragment.createInstance().apply {
                listener = this@DigitalInvoiceExampleActivity
            }
            add(R.id.digital_invoice_screen_container, onboardingFragment, TAG_ONBOARDING)
        }
    }

    override fun onCloseOnboarding() {
        (supportFragmentManager.findFragmentByTag(TAG_ONBOARDING) as? DigitalInvoiceOnboardingFragment)?.let { infoFragment ->
            infoFragment.listener = null
            supportFragmentManager.commit {
                remove(infoFragment)
            }
        }
    }

    override fun onPayInvoice(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    ) {
        LOG.debug("Show extractions with line items")
        startActivity(ExtractionsActivity.getStartIntent(this, extractions, compoundExtractions))
        finish()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DigitalInvoiceExampleActivity::class.java)

        private const val EXTRA_IN_EXTRACTIONS = "EXTRA_IN_EXTRACTIONS"
        private const val EXTRA_IN_COMPOUND_EXTRACTIONS = "EXTRA_IN_COMPOUND_EXTRACTIONS"
        private const val EXTRA_IN_RETURN_REASONS = "EXTRA_IN_RETURN_REASONS"

        fun getStartIntent(
            context: Context,
            extractions: Map<String, GiniCaptureSpecificExtraction>,
            compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
            returnReasons: List<GiniCaptureReturnReason>
        ): Intent = Intent(context, DigitalInvoiceExampleActivity::class.java).apply {
            putExtra(EXTRA_IN_EXTRACTIONS, extractions.toBundle())
            putExtra(EXTRA_IN_COMPOUND_EXTRACTIONS, compoundExtractions.toBundle())
            putParcelableArrayListExtra(
                EXTRA_IN_RETURN_REASONS,
                ArrayList<Parcelable>(returnReasons)
            )
        }
    }
}