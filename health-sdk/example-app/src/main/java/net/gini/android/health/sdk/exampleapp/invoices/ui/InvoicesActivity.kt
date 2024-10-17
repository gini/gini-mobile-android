package net.gini.android.health.sdk.exampleapp.invoices.ui

import android.annotation.SuppressLint
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Failure
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Loading
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.moreinformation.MoreInformationFragment
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentConfiguration
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentView
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState.Error
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.ReviewFragmentListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState.Loading as LoadingBankApp

open class InvoicesActivity : AppCompatActivity() {

    private val viewModel: InvoicesViewModel by viewModel()

    private val reviewFragmentListener = object : ReviewFragmentListener {
        override fun onCloseReview() {
            supportFragmentManager.popBackStack()
        }

        override fun onToTheBankButtonClicked(paymentProviderName: String) {}
    }

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
                            if (uploadState == Loading || bankAppsState == LoadingBankApp) {
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
                        if (paymentProviderAppsState is Error) {
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
                            else -> {}
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
        binding.invoicesList.adapter = InvoicesAdapter(emptyList(), viewModel.paymentComponent)
        binding.invoicesList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        viewModel.paymentComponent.listener = object: PaymentComponent.Listener {
            override fun onMoreInformationClicked() {
                MoreInformationFragment.newInstance(viewModel.paymentComponent).apply {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_container,this, this::class.java.simpleName)
                        .addToBackStack(this::class.java.simpleName)
                        .commit()
                }
            }

            override fun onBankPickerClicked() {
                BankSelectionBottomSheet.newInstance(viewModel.paymentComponent).apply {
                    show(supportFragmentManager, BankSelectionBottomSheet::class.simpleName)
                }
            }

            override fun onPayInvoiceClicked(documentId: String) {
                LOG.debug("Pay invoice clicked")

                viewModel.getPaymentReviewFragment(documentId)
                    .onSuccess { reviewFragment ->
                        reviewFragment.listener = reviewFragmentListener

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
        }

        // Reattach the listener to the ReviewFragment if it is being shown (in case of configuration changes)
        supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)?.let {
            (it as? ReviewFragment)?.listener = reviewFragmentListener
        }

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
    private val paymentComponent: PaymentComponent
) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View, paymentComponent: PaymentComponent) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val dueDate: TextView
        val amount: TextView
        val medProvider: TextView
        val paymentComponentView: PaymentComponentView

        init {
            recipient = view.findViewById(R.id.recipient)
            dueDate = view.findViewById(R.id.due_date)
            amount = view.findViewById(R.id.amount)
            medProvider = view.findViewById(R.id.medicalServiceProvider)
            this.paymentComponentView = view.findViewById(R.id.payment_component)
            this.paymentComponentView.paymentComponent = paymentComponent
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_invoice, viewGroup, false)
        return ViewHolder(view, paymentComponent)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val invoiceItem = dataSet[position]

        viewHolder.recipient.text = invoiceItem.recipient ?: ""
        viewHolder.dueDate.text = invoiceItem.dueDate ?: ""
        viewHolder.amount.text = invoiceItem.amount ?: ""
        invoiceItem.medicalProvider?.let {
            viewHolder.medProvider.visibility = View.VISIBLE
            viewHolder.medProvider.text = "Med. provider: $it"
        }?: {
            viewHolder.medProvider.visibility = View.GONE
        }

        viewHolder.paymentComponentView.prepareForReuse()
        viewHolder.paymentComponentView.isPayable = invoiceItem.isPayable
        viewHolder.paymentComponentView.documentId = invoiceItem.documentId
    }

    override fun getItemCount() = dataSet.size
}

private const val REVIEW_FRAGMENT_TAG = "payment_review_fragment"
