package net.gini.android.health.sdk.exampleapp.invoices.ui

import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState
import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import org.koin.androidx.viewmodel.ext.android.viewModel


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
                        binding.noInvoicesLabel.visibility = if (invoicesWithExtractions.isEmpty()) View.VISIBLE else View.GONE
                        Log.d(this::class.simpleName, "Invoices with extractions: $invoicesWithExtractions")
                    }
                }
                launch {
                    viewModel.uploadHardcodedInvoicesState.collect { uploadState ->
                        when(uploadState) {
                            is UploadHardcodedInvoicesState.Failure -> {
                                AlertDialog.Builder(this@InvoicesActivity)
                                    .setTitle(R.string.upload_failed)
                                    .setMessage(uploadState.errors.toSet().joinToString(", "))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }
                            UploadHardcodedInvoicesState.Idle,
                            UploadHardcodedInvoicesState.Success -> {
                                binding.loadingIndicatorContainer.visibility = View.INVISIBLE
                                binding.loadingIndicator.visibility = View.INVISIBLE
                            }
                            UploadHardcodedInvoicesState.Loading -> {
                                binding.loadingIndicatorContainer.visibility = View.VISIBLE
                                binding.loadingIndicator.visibility = View.VISIBLE
                            }
                        }
                        Log.d(this::class.simpleName, "Upload state: $uploadState")
                    }
                }
            }
        }

        viewModel.loadInvoicesWithExtractions()

        binding.invoicesList.layoutManager = LinearLayoutManager(this)
        binding.invoicesList.adapter = InvoicesAdapter(emptyList())
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

class InvoicesAdapter(var dataSet: List<InvoiceItem>) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.recipient.text = dataSet[position].recipient ?: ""
        viewHolder.dueDate.text = dataSet[position].dueDate ?: ""
        viewHolder.amount.text = dataSet[position].amount ?: ""
    }

    override fun getItemCount() = dataSet.size
}