package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.MainActivity
import net.gini.android.merchant.sdk.exampleapp.R
import net.gini.android.merchant.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.merchant.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.merchant.sdk.integratedFlow.IntegratedFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.IntegratedPaymentContainerFragment
import net.gini.android.merchant.sdk.moreinformation.MoreInformationFragment
import net.gini.android.merchant.sdk.review.ReviewFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory
import net.gini.android.merchant.sdk.paymentcomponent.PaymentProviderAppsState.Loading as LoadingBankApp

class InvoicesActivity : AppCompatActivity() {

    private val viewModel: InvoicesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInvoicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActivityTitle()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.invoicesFlow.collect { invoicesWithExtractions ->
                        (binding.invoicesList.adapter as InvoicesAdapter).apply {
                            dataSet = invoicesWithExtractions
                            notifyDataSetChanged()
                        }
                        binding.noInvoicesLabel.visibility =
                            if (invoicesWithExtractions.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.uploadHardcodedInvoicesStateFlow.combine(viewModel.paymentProviderAppsFlow) { a, b -> a to b }
                        .collect { (uploadState, bankAppsState) ->
                            if (uploadState == UploadHardcodedInvoicesState.Loading || bankAppsState == LoadingBankApp) {
                                showLoadingIndicator(binding)
                            } else {
                                hideLoadingIndicator(binding)
                            }
                        }
                }
                launch {
                    viewModel.uploadHardcodedInvoicesStateFlow.collect { uploadState ->
                        if (uploadState is UploadHardcodedInvoicesState.Failure) {
                            AlertDialog.Builder(this@InvoicesActivity)
                                .setTitle(R.string.upload_failed)
                                .setMessage(uploadState.errors.toSet().joinToString(", "))
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }

                    }
                }
                launch {
                    viewModel.paymentProviderAppsFlow.collect { paymentProviderAppsState ->
                        if (paymentProviderAppsState is Error) {
                            AlertDialog.Builder(this@InvoicesActivity)
                                .setTitle(R.string.failed_to_load_bank_apps)
//                                .setMessage(paymentProviderAppsState.throwable.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }

                    }
                }
                launch {
                    viewModel.openBankState.collect { paymentState ->
                        when (paymentState) {
                            is GiniMerchant.PaymentState.Success -> {
                                viewModel.updateDocument()
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.startIntegratedPaymentFlow.collect { containerFragment ->
                        startIntegratedPaymentFlow(containerFragment)
                    }
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.loadInvoicesWithExtractions()
        viewModel.loadPaymentProviderApps()

        IntentCompat.getParcelableExtra(intent, MainActivity.FLOW_CONFIGURATION, IntegratedFlowConfiguration::class.java)?.let {
            viewModel.setIntegratedFlowConfiguration(it)
        }

        binding.invoicesList.layoutManager = LinearLayoutManager(this)
        binding.invoicesList.adapter = InvoicesAdapter(emptyList()) { invoiceItem ->
            viewModel.setSelectedInvoiceItem(invoiceItem)
            showInvoiceDetailsFragment()
        }
        binding.invoicesList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        supportFragmentManager.addOnBackStackChangedListener {
            setActivityTitle()
            invalidateOptionsMenu()
        }
    }

    private fun setActivityTitle() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            title = getString(R.string.title_activity_invoices)
        } else if (supportFragmentManager.fragments.last() is MoreInformationFragment) {
            title =
                getString(net.gini.android.merchant.sdk.R.string.gms_more_information_fragment_title)
        } else if (supportFragmentManager.fragments.last() is ReviewFragment) {
            title = getString(R.string.title_payment_review)
        } else if (supportFragmentManager.fragments.last() is InvoiceDetailsFragment) {
            title = "Invoice details"
        }
    }
    private fun hideLoadingIndicator(binding: ActivityInvoicesBinding) {
        binding.loadingIndicatorContainer.visibility = View.INVISIBLE
        binding.loadingIndicator.visibility = View.INVISIBLE
    }

    private fun showLoadingIndicator(binding: ActivityInvoicesBinding) {
        binding.loadingIndicatorContainer.visibility = View.VISIBLE
        binding.loadingIndicator.visibility = View.VISIBLE
    }

    private fun showInvoiceDetailsFragment() {
        InvoiceDetailsFragment.newInstance().apply {
            add()
        }
    }

    private fun startIntegratedPaymentFlow(containerFragment: IntegratedPaymentContainerFragment) {
        containerFragment.apply {
            add()
        }
    }

    private fun Fragment.add() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, this, this::class.java.simpleName)
            .addToBackStack(this::class.java.simpleName)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) {
            menuInflater.inflate(R.menu.invoices_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.upload_test_invoices -> {
                viewModel.uploadHardcodedInvoices()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InvoicesActivity::class.java)
    }
}

class InvoicesAdapter(
    var dataSet: List<InvoiceItem>,
    private val openInvoiceDetails: (InvoiceItem) -> Unit,
) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val dueDate: TextView
        val amount: TextView

        init {
            recipient = view.findViewById(R.id.recipient)
            dueDate = view.findViewById(R.id.due_date)
            amount = view.findViewById(R.id.amount)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_invoice, viewGroup, false)
        return ViewHolder(view).also {  vh ->
            vh.itemView.setOnClickListener {
                openInvoiceDetails(dataSet[vh.adapterPosition])
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val invoiceItem = dataSet[position]

        viewHolder.recipient.text = invoiceItem.recipient ?: ""
        viewHolder.dueDate.text = invoiceItem.dueDate ?: ""
        viewHolder.amount.text = invoiceItem.amount ?: ""

    }

    override fun getItemCount() = dataSet.size
}

private const val REVIEW_FRAGMENT_TAG = "payment_review_fragment"
