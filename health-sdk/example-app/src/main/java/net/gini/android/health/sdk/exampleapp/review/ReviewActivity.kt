package net.gini.android.health.sdk.exampleapp.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.MainActivity.Companion.PAYMENT_COMPONENT_CONFIG
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityReviewBinding
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesActivity
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesActivity.Companion
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.ReviewFragmentListener
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.internal.payment.bankselection.BankSelectionBottomSheet
import net.gini.android.internal.payment.moreinformation.MoreInformationFragment
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentComponentConfiguration
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.review.ReviewConfiguration
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class ReviewActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModel()
    private var doctorName: SpecificExtraction? = null

    private val reviewFragmentListener = object : ReviewFragmentListener {
        override fun onCloseReview() {
            LOG.debug("on close clicked")
            finish()
        }

        override fun onToTheBankButtonClicked(paymentProviderName: String) {
            LOG.debug("to the bank button clicked with payment provider: {}", paymentProviderName)
            lifecycleScope.launch {
                viewModel.giniHealth.openBankState.collect { paymentState ->
                    when (paymentState) {
                        GiniHealth.PaymentState.Loading -> {
                            LOG.debug("opening bank app")
                        }

                        is GiniHealth.PaymentState.Success -> {
                            LOG.debug("launching bank app: {}", paymentState.paymentRequest.bankApp.name)
                            cancel()
                        }

                        is GiniHealth.PaymentState.Error -> {
                            LOG.error( "failed to open bank app:", paymentState.throwable)
                            cancel()
                        }

                        GiniHealth.PaymentState.NoAction -> {}
                    }
                }
            }
        }
    }

    /**
     * Set it to `true` to show the close button instead of the toolbar.
     *
     * When 'false' then also set the dimen resource `gpb_page_padding_top` to equal
     * `toolbar_height` to prevent the toolbar overlapping the top of the page.
     */
    private val showCloseButton = true

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.giniHealth.setSavedStateRegistryOwner(this, viewModel.viewModelScope)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        val binding = ActivityReviewBinding.inflate(LayoutInflater.from(baseContext))
        IntentCompat.getParcelableExtra(intent, PAYMENT_COMPONENT_CONFIG, PaymentComponentConfiguration::class.java)?.let {
            viewModel.setPaymentComponentConfig(it)
        }

        setContentView(binding.root)

        binding.toolbar.isGone = showCloseButton

        binding.toolbar.applyInsetter {
            type(statusBars = true) {
                padding(top = true)
            }
        }

        binding.reviewFragment.applyInsetter {
            type(statusBars = true, navigationBars = true) {
                padding(top = true, bottom = true)
            }
        }

        // Set a listener on the PaymentComponent to receive events from the PaymentComponentView

        viewModel.giniPaymentModule.paymentComponent.listener = object: PaymentComponent.Listener {
            override fun onMoreInformationClicked() {
                MoreInformationFragment.newInstance(viewModel.giniPaymentModule.paymentComponent).apply {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.review_fragment,this, this::class.java.simpleName)
                        .addToBackStack(this::class.java.simpleName)
                        .commit()
                }
            }

            override fun onBankPickerClicked() {
                BankSelectionBottomSheet.newInstance(viewModel.giniPaymentModule.paymentComponent).apply {
                    show(supportFragmentManager, BankSelectionBottomSheet::class.simpleName)
                }
            }

            override fun onPayInvoiceClicked(documentId: String?) {
                if (documentId == null) return
                // Get and show the payment ReviewFragment for the document id
                lifecycleScope.launch {
                    binding.progress.visibility = View.VISIBLE

                    try {
                        val reviewFragment = viewModel.giniHealth.getPaymentReviewFragment(
                            documentId = documentId,
                            viewModel.giniPaymentModule.paymentComponent,
                            configuration = ReviewConfiguration(showCloseButton = showCloseButton)
                        )

                        reviewFragment.listener = reviewFragmentListener

                        supportFragmentManager.commit {
                            replace(R.id.review_fragment, reviewFragment, REVIEW_FRAGMENT_TAG)
                        }

                        doctorName?.value.let {
                            Toast.makeText(this@ReviewActivity, "Extracted doctor's name: $it", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        AlertDialog.Builder(this@ReviewActivity)
                            .setTitle(getString(R.string.could_not_start_payment_review))
                            .setMessage(e.message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    } finally {
                        binding.progress.visibility = View.INVISIBLE
                    }
                }
            }
        }

        // The PaymentComponentView needs the PaymentComponent to be set before it is shown
        binding.paymentComponentView.paymentComponent = viewModel.giniPaymentModule.paymentComponent

        lifecycleScope.launch {
            val documentId = (viewModel.giniHealth.documentFlow.value as ResultWrapper.Success<Document>).value.id

            val isDocumentPayable = viewModel.giniHealth.checkIfDocumentIsPayable(documentId)
            val containsMultipleDocuments = viewModel.giniHealth.checkIfDocumentContainsMultipleDocuments(documentId)

            if (!isDocumentPayable || containsMultipleDocuments) {
                val alertTitle = when {
                    !isDocumentPayable && containsMultipleDocuments -> {
                        getString(R.string.multiple_documents) + " & " + getString(R.string.document_not_payable_title)
                    }
                    !isDocumentPayable -> {
                        getString(R.string.document_not_payable_title)
                    }
                    else -> {
                        getString(R.string.multiple_documents)
                    }
                }

                AlertDialog.Builder(this@ReviewActivity)
                    .setTitle(alertTitle)
                    .setMessage(R.string.document_not_payable_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ -> finish() }
                    .setOnDismissListener {
                        finish()
                    }
                    .show()
                return@launch
            }

            // Configure the PaymentComponentView
            binding.paymentComponentView.isPayable = true
            binding.paymentComponentView.documentId = documentId

            // Load the payment provider apps and show an alert dialog for errors
            viewModel.loadPaymentProviderApps()

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.paymentProviderAppsFlow.collect { paymentProviderAppsState ->
                        when (paymentProviderAppsState) {
                            is PaymentProviderAppsState.Error -> {
                                binding.progress.visibility = View.INVISIBLE

                                AlertDialog.Builder(this@ReviewActivity)
                                    .setTitle(R.string.failed_to_load_bank_apps)
                                    .setMessage(paymentProviderAppsState.throwable.message)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }

                            PaymentProviderAppsState.Loading -> {
                                binding.progress.visibility = View.VISIBLE
                            }

                            is PaymentProviderAppsState.Success -> {
                                binding.progress.visibility = View.INVISIBLE
                            }

                            PaymentProviderAppsState.Nothing -> return@collect
                        }
                    }
                }

                launch {
                    viewModel.giniHealth.paymentFlow.collect { extractedPaymentDetails ->
                        if (extractedPaymentDetails is ResultWrapper.Success) {
                            doctorName = extractedPaymentDetails.value.extractions?.specificExtractions?.get(MED_PROVIDER)
                        }
                    }
                }
            }
        }

        binding.close.setOnClickListener { finish() }

        // Reattach the listener to the ReviewFragment if it is being shown (in case of configuration changes)
        supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)?.let {
            (it as? ReviewFragment)?.listener = reviewFragmentListener
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewActivity::class.java)

        private const val EXTRA_URIS = "EXTRA_URIS"
        private const val MED_PROVIDER = "medical_service_provider"

        fun getStartIntent(context: Context, pages: List<Uri> = emptyList(), paymentComponentConfiguration: PaymentComponentConfiguration?): Intent =
            Intent(context, ReviewActivity::class.java).apply {
                putParcelableArrayListExtra(
                    EXTRA_URIS,
                    if (pages is ArrayList<Uri>) pages else ArrayList<Uri>().apply { addAll(pages) })
                putExtra(PAYMENT_COMPONENT_CONFIG, paymentComponentConfiguration)
            }

        private val Intent.pageUris: List<Uri>
            get() = getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.toList() ?: emptyList()
    }
}

private const val REVIEW_FRAGMENT_TAG = "payment_review_fragment"
