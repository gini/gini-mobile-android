package net.gini.android.health.sdk.exampleapp.invoices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Failure
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Loading
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.exampleapp.orders.OrderDetailsFragment
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.internal.payment.moreinformation.MoreInformationFragment
import net.gini.android.internal.payment.paymentComponent.PaymentComponentConfiguration
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

open class InvoicesActivity : AppCompatActivity() {

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
                            if (uploadState == Loading || bankAppsState == PaymentProviderAppsState.Loading) {
                                showLoadingIndicator(binding)
                            } else {
                                hideLoadingIndicator(binding)
                            }
                        }
                }
                launch {
                    viewModel.uploadHardcodedInvoicesStateFlow.collect { uploadState ->
                        if (uploadState is Failure) {
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
                        if (paymentProviderAppsState is PaymentProviderAppsState.Error) {
                            AlertDialog.Builder(this@InvoicesActivity)
                                .setTitle(R.string.failed_to_load_bank_apps)
                                .setMessage(paymentProviderAppsState.throwable.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }

                    }
                }
                launch {
                    viewModel.openBankState.collect { paymentState ->
                        when (paymentState) {
                            is GiniHealth.PaymentState.Success -> {
                                viewModel.updateDocument()
                                supportFragmentManager.popBackStack()
                            }
                            is GiniHealth.PaymentState.Cancel -> {
                                supportFragmentManager.popBackStack()
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.startIntegratedPaymentFlow.collect { result ->
                        viewModel.getPaymentFragmentForPaymentDetails(result, IntentCompat.getParcelableExtra(intent, MainActivity.PAYMENT_FLOW_CONFIGURATION, PaymentFlowConfiguration::class.java))
                            .onSuccess { paymentFragment ->
                                supportFragmentManager.beginTransaction()
                                    .add(R.id.fragment_container, paymentFragment, REVIEW_FRAGMENT_TAG)
                                    .addToBackStack(paymentFragment::class.java.name)
                                    .commit()
                            }
                            .onFailure { error ->
                                LOG.error("Error getting payment review fragment", )
                                AlertDialog.Builder(this@InvoicesActivity)
                                    .setTitle(getString(R.string.could_not_start_payment_review))
                                    .setMessage(error.message)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }
                    }
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        IntentCompat.getParcelableExtra(intent, MainActivity.PAYMENT_COMPONENT_CONFIG, PaymentComponentConfiguration::class.java)?.let {
            viewModel.setPaymentComponentConfig(it)
        }

        viewModel.loadInvoicesWithExtractions()
        viewModel.loadPaymentProviderApps()

        binding.invoicesList.layoutManager = LinearLayoutManager(this)
        binding.invoicesList.adapter = InvoicesAdapter(emptyList()) { documentId ->
            startPaymentFlowForDocumentId(documentId)
        }
        binding.invoicesList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        supportFragmentManager.addOnBackStackChangedListener {
            setActivityTitle()
            invalidateOptionsMenu()
        }
    }

    private fun setActivityTitle() {
        if (supportFragmentManager.fragments.isEmpty()) {
            title = getString(R.string.title_activity_invoices)
        } else if (supportFragmentManager.fragments.last() is MoreInformationFragment) {
            title =
                getString(net.gini.android.health.sdk.R.string.ghs_more_information_fragment_title)
        } else if (supportFragmentManager.fragments.last() is ReviewFragment) {
            title = getString(R.string.title_payment_review)
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

    private fun startPaymentFlowForDocumentId(documentId: String) {
        viewModel.loadPaymentProviderApps()
        viewModel.getPaymentReviewFragment(documentId)
            .onSuccess { reviewFragment ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, reviewFragment, REVIEW_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit()
            }
            .onFailure { throwable ->
                LOG.error("Error getting payment review fragment", throwable)
                AlertDialog.Builder(this@InvoicesActivity)
                    .setTitle(getString(R.string.could_not_start_payment_review))
                    .setMessage(throwable.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
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
            R.id.create_payment_order -> {
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, OrderDetailsFragment.newInstance(), REVIEW_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit()
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
    var listener: (String) -> Unit
) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val dueDate: TextView
        val amount: TextView
        val payInvoiceButton: Button
        init {
            recipient = view.findViewById(R.id.recipient)
            dueDate = view.findViewById(R.id.due_date)
            amount = view.findViewById(R.id.amount)
            payInvoiceButton = view.findViewById(R.id.pay_invoice_button)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_invoice, viewGroup, false)
        val viewHolder =  ViewHolder(view)
        viewHolder.payInvoiceButton.setOnClickListener {
            listener.invoke(dataSet[viewHolder.adapterPosition].documentId)
        }
        return viewHolder
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
