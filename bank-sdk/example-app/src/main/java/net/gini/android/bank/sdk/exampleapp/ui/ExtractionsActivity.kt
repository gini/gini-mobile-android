package net.gini.android.bank.sdk.exampleapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.DefaultNetworkServicesProvider
import net.gini.android.bank.sdk.exampleapp.databinding.ActivityExtractionsBinding
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.view.TransactionDocsView
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.util.protectViewFromInsets
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 *
 * A menu item is added to send transfer summary.
 */

@AndroidEntryPoint
class ExtractionsActivity : AppCompatActivity(), ExtractionsAdapter.ExtractionsAdapterInterface {

    private lateinit var binding: ActivityExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = hashMapOf()
    private lateinit var mExtractionsAdapter: ExtractionsAdapter

    @Inject
    internal lateinit var defaultNetworkServicesProvider: DefaultNetworkServicesProvider

    private val viewModel: ExtractionsViewModel by viewModels()

    // {extraction name} to it's {entity name}
    private val editableSpecificExtractions = hashMapOf(
        "paymentRecipient" to "companyname",
        "paymentReference" to "reference",
        "paymentPurpose" to "text",
        "iban" to "iban",
        "bic" to "bic",
        "amountToPay" to "amount",
        "instantPayment" to "text"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtractionsBinding.inflate(layoutInflater)
        binding.root.protectViewFromInsets()
        setContentView(binding.root)
        readExtras()
        showAnalyzedDocumentId()
        setUpRecyclerView(binding)

        // For "open with" (file import) tests
        (applicationContext as ExampleApp).decrementIdlingResourceForOpenWith()
    }

    private fun showAnalyzedDocumentId() {
        val documentId =
            defaultNetworkServicesProvider.defaultNetworkServiceDebugDisabled.analyzedGiniApiDocument?.id
                ?: defaultNetworkServicesProvider.defaultNetworkServiceDebugDisabled.analyzedGiniApiDocument?.id
                ?: ""
        binding.textDocumentId.text = getString(R.string.analyzed_document_id, documentId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_extractions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.transfer_summary -> {
            if (isCaptureSDKExtractions)
                sendTransferSummaryAndCloseForCapture()
            else
                sendTransferSummaryAndClose(binding)

            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun valueChanged(key: String, newValue: String) {
        mExtractions[key]?.apply {
            value = newValue
        }
    }

    private fun readExtras() {
        intent.extras?.getParcelable<Bundle>(EXTRA_IN_EXTRACTIONS)?.run {
            keySet().forEach { name ->
                getParcelable<GiniCaptureSpecificExtraction>(name)?.let { mExtractions[name] = it }
            }
        }
    }

    private fun setUpRecyclerView(binding: ActivityExtractionsBinding) {
        binding.recyclerviewExtractions.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ExtractionsActivity)

            editableSpecificExtractions.forEach {
                if (!mExtractions.containsKey(it.key)) {
                    mExtractions[it.key] = GiniCaptureSpecificExtraction(
                        it.key, "", it.value, null, emptyList()
                    )
                }
            }

            adapter = ExtractionsAdapter(
                getSortedExtractions(mExtractions),
                this@ExtractionsActivity,
                editableSpecificExtractions.keys.toList()
            ).also {
                mExtractionsAdapter = it
            }
            setOnTouchListener { _, _ ->
                performClick()
                val inputMethodManager =
                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                false
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> =
        extractions.toSortedMap().values.toList()

    private fun sendTransferSummaryAndClose(binding: ActivityExtractionsBinding) {
        // Transfer summary should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        var amount = mExtractions["amountToPay"]?.value ?: ""
        val paymentRecipient = mExtractions["paymentRecipient"]?.value ?: ""
        val paymentReference = mExtractions["paymentReference"]?.value ?: ""
        val paymentPurpose = mExtractions["paymentPurpose"]?.value ?: ""
        val iban = mExtractions["iban"]?.value ?: ""
        val bic = mExtractions["bic"]?.value ?: ""
        val instantPayment = mExtractions["instantPayment"]?.value ?: ""

        if (amount.isEmpty()) {
            amount = Amount.EMPTY.amountToPay()
        }

        viewModel.saveTransactionData(
            iban,
            bic,
            amount,
            paymentRecipient,
            paymentPurpose,
            paymentReference,
        )

        GiniBank.sendTransferSummary(
            paymentRecipient,
            paymentReference,
            paymentPurpose,
            iban,
            bic,
            Amount(
                BigDecimal(amount.removeSuffix(":EUR")), AmountCurrency.EUR
            ),
            instantPayment.toBooleanStrictOrNull()
        )

        GiniBank.cleanupCapture(applicationContext)

        finish()
    }

    private fun sendTransferSummaryAndCloseForCapture() {
        // Transfer summary should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        var amount = mExtractions["amountToPay"]?.value ?: ""
        val paymentRecipient = mExtractions["paymentRecipient"]?.value ?: ""
        val paymentReference = mExtractions["paymentReference"]?.value ?: ""
        val paymentPurpose = mExtractions["paymentPurpose"]?.value ?: ""
        val iban = mExtractions["iban"]?.value ?: ""
        val bic = mExtractions["bic"]?.value ?: ""
        val instantPayment = mExtractions["instantPayment"]?.value ?: ""

        if (amount.isEmpty()) {
            amount = Amount.EMPTY.amountToPay()
        }

        GiniCapture.sendTransferSummary(
            paymentRecipient,
            paymentReference,
            paymentPurpose,
            iban,
            bic,
            Amount(
                BigDecimal(amount.removeSuffix(":EUR")), AmountCurrency.EUR
            ),
            instantPayment.toBooleanStrictOrNull()
        )

        GiniCapture.cleanup(applicationContext)

        finish()
    }

    private fun showProgressIndicator(binding: ActivityExtractionsBinding) {
        binding.recyclerviewExtractions.animate().alpha(0.5f)
        binding.layoutProgress.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator(binding: ActivityExtractionsBinding) {
        binding.recyclerviewExtractions.animate().alpha(1.0f)
        binding.layoutProgress.visibility = View.GONE
    }

    companion object {
        const val EXTRA_IN_EXTRACTIONS = "EXTRA_IN_EXTRACTIONS"
        var isCaptureSDKExtractions : Boolean = false
        fun getStartIntent(
            context: Context, extractionsBundle: Map<String, GiniCaptureSpecificExtraction>,
            isCaptureSdkExtractions: Boolean = false
        ): Intent {
            isCaptureSDKExtractions = isCaptureSdkExtractions
            return Intent(context, ExtractionsActivity::class.java).apply {
                putExtra(EXTRA_IN_EXTRACTIONS, Bundle().apply {
                    extractionsBundle.map { putParcelable(it.key, it.value) }
                })
            }
        }
    }
}

private class ExtractionsAdapter(
    var extractions: List<GiniCaptureSpecificExtraction>,
    var listener: ExtractionsAdapterInterface? = null,
    val editableSpecificExtractions: List<String>,
) : RecyclerView.Adapter<ViewHolder>() {

    interface ExtractionsAdapterInterface {
        fun valueChanged(key: String, value: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_EXTRACTION -> {
                ExtractionsViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_extraction, parent, false)
                ).also { holder ->
                    holder.mTextValue.addTextChangedListener {
                        listener?.valueChanged(
                            holder.mTextInputLayout.hint.toString(),
                            it.toString()
                        )
                    }
                }
            }

            TYPE_TRANSACTION_DOCS -> {
                ExtractionsDocsViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_transaction_docs, parent, false)
                ).apply {
                    this.transactionDocView.onDocumentClick { doc, infoTextLines ->
                        this.itemView.context.startActivity(
                            TransactionDocInvoicePreviewActivity.newIntent(
                                screenTitle = doc.documentFileName,
                                context = this.itemView.context,
                                documentId = doc.giniApiDocumentId,
                                infoTextLines = infoTextLines
                            )
                        )
                    }
                }
            }

            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ExtractionsViewHolder -> {
                extractions.getOrNull(position)?.run {
                    holder.mTextValue.setText(value)
                    holder.mTextInputLayout.hint = name
                    holder.mTextValue.isEnabled = name in editableSpecificExtractions
                }
            }
        }
    }

    override fun getItemCount(): Int = extractions.size + 1 // +1 for a TransactionDocView

    override fun getItemViewType(position: Int): Int {
        return when {
            position in 0..extractions.lastIndex -> TYPE_EXTRACTION
            else -> TYPE_TRANSACTION_DOCS
        }
    }

    companion object {
        const val TYPE_EXTRACTION = 0
        const val TYPE_TRANSACTION_DOCS = 1
    }
}

private class ExtractionsViewHolder(itemView: View) : ViewHolder(itemView) {
    var mTextInputLayout: TextInputLayout = itemView.findViewById(R.id.text_input_layout)
    var mTextValue: TextInputEditText = itemView.findViewById(R.id.text_value)
}

private class ExtractionsDocsViewHolder(itemView: View) : ViewHolder(itemView) {
    val transactionDocView: TransactionDocsView = itemView.findViewById(R.id.transaction_docs_view)
}
