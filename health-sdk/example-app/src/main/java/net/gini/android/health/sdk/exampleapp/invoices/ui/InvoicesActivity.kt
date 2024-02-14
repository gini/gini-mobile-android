package net.gini.android.health.sdk.exampleapp.invoices.ui

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Failure
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Loading
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentView
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentsController
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState.Error
import org.koin.androidx.viewmodel.ext.android.viewModel
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState.Loading as LoadingBankApp


class InvoicesActivity : AppCompatActivity() {

    private val viewModel: InvoicesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInvoicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    viewModel.uploadHardcodedInvoicesStateFlow.combine(viewModel.bankAppsFlow) { a, b -> a to b }
                        .collect { (uploadState, bankAppsState) ->
                            if (uploadState == Loading || bankAppsState == LoadingBankApp) {
                                binding.loadingIndicatorContainer.visibility = View.VISIBLE
                                binding.loadingIndicator.visibility = View.VISIBLE
                            } else {
                                binding.loadingIndicatorContainer.visibility = View.INVISIBLE
                                binding.loadingIndicator.visibility = View.INVISIBLE
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
                    viewModel.bankAppsFlow.collect { bankAppsState ->
                        if (bankAppsState is Error) {
                            AlertDialog.Builder(this@InvoicesActivity)
                                .setTitle(R.string.failed_to_load_bank_apps)
                                .setMessage(bankAppsState.throwable.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }

                    }
                }
            }
        }

        viewModel.loadInvoicesWithExtractions()
        viewModel.loadPaymentProviderApps()

        binding.invoicesList.layoutManager = LinearLayoutManager(this)
        binding.invoicesList.adapter = InvoicesAdapter(emptyList(), viewModel.paymentComponentsController)
        binding.invoicesList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.invoices_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.upload_test_invoices -> {
                viewModel.uploadHardcodedInvoices()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}

class InvoicesAdapter(
    var dataSet: List<InvoiceItem>,
    private val paymentComponentsController: PaymentComponentsController
) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View, paymentComponentsController: PaymentComponentsController) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val dueDate: TextView
        val amount: TextView
        val paymentComponent: PaymentComponentView

        init {
            recipient = view.findViewById(R.id.recipient)
            dueDate = view.findViewById(R.id.due_date)
            amount = view.findViewById(R.id.amount)
            paymentComponent = view.findViewById(R.id.payment_component)
            paymentComponent.paymentComponentsController = paymentComponentsController
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_invoice, viewGroup, false)
        return ViewHolder(view, paymentComponentsController)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val invoiceItem = dataSet[position]
        viewHolder.recipient.text = invoiceItem.recipient ?: ""
        viewHolder.dueDate.text = invoiceItem.dueDate ?: ""
        viewHolder.amount.text = invoiceItem.amount ?: ""

        viewHolder.paymentComponent.prepareForReuse()
        viewHolder.paymentComponent.isPayable = invoiceItem.isPayable
    }

    override fun getItemCount() = dataSet.size
}