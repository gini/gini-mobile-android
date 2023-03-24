package net.gini.android.bank.sdk.screenapiexample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.bank.sdk.screenapiexample.databinding.ActivityExtractionsBinding
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import org.koin.android.ext.android.inject
import androidx.core.widget.addTextChangedListener


/**
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 *
 * A menu item is added to send feedback. The amount is changed to 10.00:EUR or an amount of
 * 10.00:EUR is added, if missing.
 */
class ExtractionsActivity : AppCompatActivity(), ExtractionsAdapter.ExtractionsAdapterInterface {
    private lateinit var binding: ActivityExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = hashMapOf()
    private lateinit var mExtractionsAdapter: ExtractionsAdapter
    private val defaultNetworkService: GiniCaptureDefaultNetworkService by inject()

    // {extraction name} to it's {entity name}
    private val editableSpecificExtractions = hashMapOf("paymentRecipient" to "companyname", "paymentReference" to "reference" ,
        "paymentPurpose" to "text", "iban" to "iban", "bic" to "bic", "amountToPay" to "amount")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtractionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readExtras()
        showAnalyzedDocumentId()
        setUpRecyclerView(binding)
    }

    private fun showAnalyzedDocumentId() {
        val documentId = defaultNetworkService.analyzedGiniApiDocument?.id ?: ""
        binding.textDocumentId.text = getString(R.string.analyzed_document_id, documentId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_extractions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.feedback -> {
            sendFeedback(binding)
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
                        it.key, "",
                        it.value, null, emptyList())
                }
            }

            adapter = ExtractionsAdapter(getSortedExtractions(mExtractions), this@ExtractionsActivity, editableSpecificExtractions.keys.toList()).also {
                mExtractionsAdapter = it
            }
            setOnTouchListener { _, _ ->
                performClick()
                val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> = extractions.toSortedMap().values.toList()

    private fun sendFeedback(binding: ActivityExtractionsBinding) {
        // Feedback should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        var amount = mExtractions["amountToPay"]?.value ?: ""
        if (amount.isEmpty()) {
            mExtractions["amountToPay"]?.value = "0.0:EUR"
            mExtractionsAdapter.extractions = getSortedExtractions(mExtractions)
        }

        mExtractionsAdapter.notifyDataSetChanged()
        showProgressIndicator(binding)
        val giniCaptureNetworkApi = GiniCapture.getInstance().giniCaptureNetworkApi
        if (giniCaptureNetworkApi == null) {
            Toast.makeText(
                this, "Feedback not sent: missing GiniCaptureNetworkApi implementation.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        giniCaptureNetworkApi.sendFeedback(mExtractions, object : GiniCaptureNetworkCallback<Void, Error> {
            override fun failure(error: Error) {
                hideProgressIndicator(binding)
                Toast.makeText(
                    this@ExtractionsActivity,
                    "Feedback error:\n" + error.message,
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun success(result: Void?) {
                hideProgressIndicator(binding)
                Toast.makeText(
                    this@ExtractionsActivity,
                    "Feedback successful",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun cancelled() {
                hideProgressIndicator(binding)
            }
        })
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

        fun getStartIntent(context: Context, extractionsBundle: Map<String, GiniCaptureSpecificExtraction>): Intent =
            Intent(context, ExtractionsActivity::class.java).apply {
                putExtra(EXTRA_IN_EXTRACTIONS, Bundle().apply {
                    extractionsBundle.map { putParcelable(it.key, it.value) }
                })
            }
    }
}

private class ExtractionsAdapter(var extractions: List<GiniCaptureSpecificExtraction>,
                                 var listener: ExtractionsAdapterInterface? = null,
                                 val editableSpecificExtractions : List<String>) : RecyclerView.Adapter<ExtractionsViewHolder>() {

    interface ExtractionsAdapterInterface {
        fun valueChanged(key: String, value: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtractionsViewHolder {
        val holder = ExtractionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_extraction, parent, false))
        holder.mTextValue.addTextChangedListener {
            listener?.valueChanged(holder.mTextInputLayout.hint.toString(), it.toString())
        }

        return holder
    }

    override fun onBindViewHolder(holder: ExtractionsViewHolder, position: Int) {
        extractions.getOrNull(position)?.run {
            holder.mTextValue.setText(value)
            holder.mTextInputLayout.hint = name
            holder.mTextValue.isEnabled = name in editableSpecificExtractions
        }
    }

    override fun getItemCount(): Int = extractions.size
}

private class ExtractionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var mTextInputLayout: TextInputLayout = itemView.findViewById(R.id.text_input_layout)
    var mTextValue: TextInputEditText = itemView.findViewById(R.id.text_value)
}
