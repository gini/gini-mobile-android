package net.gini.android.bank.sdk.screenapiexample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.bank.sdk.screenapiexample.R
import net.gini.android.bank.sdk.screenapiexample.databinding.ActivityExtractionsBinding
import net.gini.android.capture.network.GiniCaptureDefaultNetworkApi
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.koin.android.ext.android.inject

/**
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 *
 * A menu item is added to send feedback. The amount is changed to 10.00:EUR or an amount of
 * 10.00:EUR is added, if missing.
 */
class ExtractionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = hashMapOf()
    private lateinit var mExtractionsAdapter: ExtractionsAdapter
    private val defaultNetworkService: GiniCaptureDefaultNetworkService by inject()

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
            adapter = ExtractionsAdapter(getSortedExtractions(mExtractions)).also {
                mExtractionsAdapter = it
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> = extractions.toSortedMap().values.toList()

    private fun sendFeedback(binding: ActivityExtractionsBinding) {
        // An example for sending feedback where we change the amount or add one if it is missing
        // Feedback should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        val amount = mExtractions["amountToPay"]
        if (amount != null) { // Let's assume the amount was wrong and change it
            amount.value = "10.00:EUR"
            Toast.makeText(this, "Amount changed to 10.00:EUR", Toast.LENGTH_SHORT).show()
        } else { // Amount was missing, let's add it
            mExtractions["amountToPay"] = GiniCaptureSpecificExtraction("amountToPay", "10.00:EUR", "amount", null, emptyList())
            mExtractionsAdapter.extractions = getSortedExtractions(mExtractions)
            Toast.makeText(this, "Added amount of 10.00:EUR", Toast.LENGTH_SHORT).show()
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

private class ExtractionsAdapter(var extractions: List<GiniCaptureSpecificExtraction>) : RecyclerView.Adapter<ExtractionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtractionsViewHolder =
        ExtractionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_extraction, parent, false))

    override fun onBindViewHolder(holder: ExtractionsViewHolder, position: Int) {
        extractions.getOrNull(position)?.run {
            holder.mTextName.text = name
            holder.mTextValue.text = value
        }
    }

    override fun getItemCount(): Int = extractions.size
}

private class ExtractionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var mTextName: TextView = itemView.findViewById<View>(R.id.text_name) as TextView
    var mTextValue: TextView = itemView.findViewById<View>(R.id.text_value) as TextView
}
