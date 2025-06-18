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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityInvoicesBinding
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Failure
import net.gini.android.health.sdk.exampleapp.invoices.data.UploadHardcodedInvoicesState.Loading
import net.gini.android.health.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.health.sdk.exampleapp.util.SharedPreferencesUtil
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.utils.DisplayedScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

open class InvoicesActivity : AppCompatActivity() {

    private val viewModel: InvoicesViewModel by viewModel()
    private lateinit var binding: ActivityInvoicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActivityTitle(DisplayedScreen.Nothing)

        // Check if fragment is already visible on recreation
        val fragment = supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)
        val isFragmentInBackStack = fragment != null && fragment.isAdded

        setSiblingViewsEnabled(!isFragmentInBackStack)
        if (isFragmentInBackStack) {
            setActivityTitle(DisplayedScreen.ReviewScreen)
        }

        supportFragmentManager.addOnBackStackChangedListener {

            val findFragment = supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)
            val isFragmentInBackStackChanged = findFragment != null && findFragment.isAdded

            setSiblingViewsEnabled(!isFragmentInBackStackChanged)

            if (!isFragmentInBackStackChanged) {
                title = resources.getString(R.string.title_activity_invoices)
            }

            invalidateOptionsMenu()
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.invoicesFlow.collect { invoicesWithExtractions ->
                        (binding.invoicesList.adapter as InvoicesAdapter).apply {
                            dataSet = invoicesWithExtractions.toMutableList()
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
                                SharedPreferencesUtil.saveStringToSharedPreferences(
                                    SharedPreferencesUtil.PAYMENTREQUEST_KEY,
                                    paymentState.paymentRequest.id,
                                    this@InvoicesActivity
                                )

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
                                    .replace(R.id.fragment_container, paymentFragment, REVIEW_FRAGMENT_TAG)
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
                launch {
                    viewModel.displayedScreen.collect { screen ->
                        setActivityTitle(screen)
                    }
                }
                launch {
                    viewModel.trustMarkersFlow.collect { response ->
                        if (response is ResultWrapper.Success) {
                            (binding.invoicesList.adapter as InvoicesAdapter).apply {
                                trustMarkerResponse = response.value
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
                launch {
                    viewModel.deleteDocumentsFlow.collect { response ->
                        response?.let { deleteDocumentErrorResponse ->
                            if (deleteDocumentErrorResponse.message != null) {
                                AlertDialog.Builder(this@InvoicesActivity)
                                    .setTitle(getString(R.string.could_not_delete_documents))
                                    .setMessage(deleteDocumentErrorResponse.message)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                                return@collect
                            }

                            var errorMessage = ""
                            deleteDocumentErrorResponse.unauthorizedDocuments?.let {
                                errorMessage += "${getString(R.string.unauthorized_documents)} $it"
                            }
                            deleteDocumentErrorResponse.notFoundDocuments?.let {
                                errorMessage += "\n${getString(R.string.not_found_documents)} $it"
                            }
                            deleteDocumentErrorResponse.missingCompositeDocuments?.let {
                                errorMessage += "\n${getString(R.string.missing_composite_documents)} $it"
                            }
                            AlertDialog.Builder(this@InvoicesActivity)
                                .setTitle(getString(R.string.could_not_delete_documents))
                                .setMessage(errorMessage)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.loadInvoicesWithExtractions()

        binding.invoicesList.layoutManager = LinearLayoutManager(this)
        binding.invoicesList.adapter = InvoicesAdapter(mutableListOf()) { documentId ->
            startPaymentFlowForDocumentId(documentId)
        }
        binding.invoicesList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                title = resources.getString(R.string.title_activity_invoices)
            }
            invalidateOptionsMenu()
        }
    }

    private fun setActivityTitle(screen: DisplayedScreen) {
        when (screen) {
            DisplayedScreen.MoreInformationFragment -> title = getString(net.gini.android.internal.payment.R.string.gps_more_information_fragment_title)
            DisplayedScreen.ReviewScreen -> title = getString(R.string.title_payment_review)
            DisplayedScreen.Nothing -> title = getString(R.string.title_activity_invoices)
            else -> {}
        }
        invalidateOptionsMenu()
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
        val paymentFlowConfiguration = IntentCompat.getParcelableExtra(intent, MainActivity.PAYMENT_FLOW_CONFIGURATION, PaymentFlowConfiguration::class.java)
        viewModel.getPaymentReviewFragment(documentId,paymentFlowConfiguration)
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

    private fun setSiblingViewsEnabled(enabled: Boolean) {
        val accessibilityFlag = if (enabled) {
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        } else {
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        // Apply to all visible items in the RecyclerView
        for (i in 0 until binding.invoicesList.childCount) {
            binding.invoicesList.getChildAt(i)?.apply {
                isFocusable = enabled
                isEnabled = enabled
                isClickable = enabled
                importantForAccessibility = accessibilityFlag
            }
        }

        binding.invoicesList.apply {
            isFocusable = enabled
            isEnabled = enabled
            isClickable = enabled
            importantForAccessibility = accessibilityFlag
        }

        binding.noInvoicesLabel.apply {
            isFocusable = enabled
            isEnabled = enabled
            isClickable = enabled
            importantForAccessibility = accessibilityFlag
        }

        binding.loadingIndicatorContainer.apply {
            isFocusable = enabled
            isEnabled = enabled
            isClickable = enabled
            importantForAccessibility = accessibilityFlag
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
            R.id.batch_delete -> {
                val adapter = binding.invoicesList.adapter as InvoicesAdapter
                val toDelete: MutableList<String> = mutableListOf()
                if (adapter.dataSet.isNotEmpty()) {
                    if (adapter.dataSet.size >= 2) {
                        toDelete.addAll(adapter.dataSet.map { it.documentId }.subList(0, 2))
                    } else {
                        toDelete.add(adapter.dataSet.first().documentId)
                    }
                }
                viewModel.batchDelete(toDelete)
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
    var dataSet: MutableList<InvoiceItem>,
    var trustMarkerResponse: GiniHealth.TrustMarkerResponse? = null,
    var listener: (String) -> Unit
) :
    RecyclerView.Adapter<InvoicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val dueDate: TextView
        val amount: TextView
        val medProvider: TextView
        val payInvoiceButton: ConstraintLayout
        val firstPaymentProviderIcon: ShapeableImageView
        val secondPaymentProviderIcon: ShapeableImageView
        val paymentProvidersCount: TextView
        init {
            recipient = view.findViewById(R.id.recipient)
            dueDate = view.findViewById(R.id.due_date)
            amount = view.findViewById(R.id.amount)
            medProvider = view.findViewById(R.id.medicalServiceProvider)
            payInvoiceButton = view.findViewById(R.id.pay_invoice_button)
            firstPaymentProviderIcon = view.findViewById(R.id.first_payment_provider_icon)
            secondPaymentProviderIcon = view.findViewById(R.id.second_payment_provider_icon)
            paymentProvidersCount = view.findViewById(R.id.extra_payment_providers_label)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_invoice, viewGroup, false)
        val viewHolder = ViewHolder(view)
        viewHolder.payInvoiceButton.setOnClickListener {
            listener.invoke(dataSet[viewHolder.adapterPosition].documentId)
        }
        return viewHolder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val invoiceItem = dataSet[position]
        viewHolder.payInvoiceButton.visibility = if (invoiceItem.isPayable) View.VISIBLE else View.GONE
        viewHolder.recipient.text = invoiceItem.recipient ?: ""
        viewHolder.dueDate.text = invoiceItem.dueDate ?: ""
        viewHolder.amount.text = invoiceItem.amount ?: ""
        invoiceItem.medicalProvider?.let {
            viewHolder.medProvider.visibility = View.VISIBLE
            viewHolder.medProvider.text = "Med. provider: $it"
        }?: {
            viewHolder.medProvider.visibility = View.GONE
        }
        trustMarkerResponse?.let {
            viewHolder.firstPaymentProviderIcon.setImageDrawable(it.paymentProviderIcon)
            viewHolder.secondPaymentProviderIcon.setImageDrawable(it.secondPaymentProviderIcon)
            viewHolder.paymentProvidersCount.text = "+${it.extraPaymentProvidersCount}"
            viewHolder.paymentProvidersCount.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = dataSet.size

    fun removeInvoices(invoices: List<String>) {
        for (invoiceId in invoices) {
            dataSet.find { it.documentId == invoiceId }?.let { dataSet.remove(it) }
        }
        notifyDataSetChanged()
    }
}

private const val REVIEW_FRAGMENT_TAG = "payment_review_fragment"
