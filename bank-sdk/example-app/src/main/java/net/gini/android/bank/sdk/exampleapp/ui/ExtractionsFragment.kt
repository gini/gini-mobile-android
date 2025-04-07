package net.gini.android.bank.sdk.exampleapp.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.FragmentExtractionsBinding
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.view.TransactionDocsView
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import java.math.BigDecimal

@AndroidEntryPoint
class ExtractionsFragment : Fragment(),  ExtractionsAdapter.ExtractionsAdapterInterface {

    private lateinit var binding: FragmentExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = hashMapOf()
    private lateinit var mExtractionsAdapter: ExtractionsAdapter

    private val configurationViewModel: ConfigurationViewModel by activityViewModels()

    // {extraction name} to it's {entity name}
    private val editableSpecificExtractions = hashMapOf(
        "paymentRecipient" to "companyname",
        "paymentReference" to "reference",
        "paymentPurpose" to "text",
        "iban" to "iban",
        "bic" to "bic",
        "amountToPay" to "amount"
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExtractionsBinding.inflate(inflater, container, false)
        return binding.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurationViewModel.extractionsBundle.forEach {
            mExtractions[it.key] = it.value
        }
        binding.toolbar.inflateMenu(R.menu.menu_extractions)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.transfer_summary -> {
                    sendTransferSummaryAndClose()
                    true
                }
                else -> false
            }
        }

        setUpRecyclerView(binding)
        handleOnBackPressed()
    }


    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainFragment()
            }
        })
    }

    private fun navigateToMainFragment() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.mainFragment, inclusive = false)
            .build()
        findNavController().navigate(R.id.mainFragment, null, navOptions)
    }

    override fun valueChanged(key: String, newValue: String) {
        mExtractions[key]?.apply {
            value = newValue
        }
    }


    private fun setUpRecyclerView(binding: FragmentExtractionsBinding) {
        binding.recyclerviewExtractions.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireActivity())

            editableSpecificExtractions.forEach {
                if (!mExtractions.containsKey(it.key)) {
                    mExtractions[it.key] = GiniCaptureSpecificExtraction(
                        it.key, "", it.value, null, emptyList()
                    )
                }
            }

            adapter = ExtractionsAdapter(
                getSortedExtractions(mExtractions),
                this@ExtractionsFragment,
                editableSpecificExtractions.keys.toList()
            ).also {
                mExtractionsAdapter = it
            }
            setOnTouchListener { _, _ ->
                performClick()
                val inputMethodManager =
                    requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> =
        extractions.toSortedMap().values.toList()

    private fun sendTransferSummaryAndClose() {
        // Transfer summary should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        var amount = mExtractions["amountToPay"]?.value ?: ""
        val paymentRecipient = mExtractions["paymentRecipient"]?.value ?: ""
        val paymentReference = mExtractions["paymentReference"]?.value ?: ""
        val paymentPurpose = mExtractions["paymentPurpose"]?.value ?: ""
        val iban = mExtractions["iban"]?.value ?: ""
        val bic = mExtractions["bic"]?.value ?: ""

        if (amount.isEmpty()) {
            amount = Amount.EMPTY.amountToPay()
        }

        GiniBank.sendTransferSummary(
            paymentRecipient, paymentReference, paymentPurpose, iban, bic, Amount(
                BigDecimal(amount.removeSuffix(":EUR")), AmountCurrency.EUR
            )
        )

        GiniBank.cleanupCapture(requireContext())

        navigateToMainFragment()
    }

    private fun showProgressIndicator(binding: FragmentExtractionsBinding) {
        binding.recyclerviewExtractions.animate().alpha(0.5f)
        binding.layoutProgress.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator(binding: FragmentExtractionsBinding) {
        binding.recyclerviewExtractions.animate().alpha(1.0f)
        binding.layoutProgress.visibility = View.GONE
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
                        parent.findNavController().navigate(
                            ExtractionsFragmentDirections.actionExtractionsFragmentToTransactionDocInvoicePreviewContainerFragment(
                                doc.giniApiDocumentId,
                                doc.documentFileName,
                                infoTextLines.toTypedArray()
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
