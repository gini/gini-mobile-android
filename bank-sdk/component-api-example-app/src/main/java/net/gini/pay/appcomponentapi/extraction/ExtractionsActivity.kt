package net.gini.pay.appcomponentapi.extraction

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
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.pay.appcomponentapi.R
import net.gini.pay.appcomponentapi.databinding.ActivityExtractionsBinding

/**
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 *
 * A menu item is added to send feedback. The amount is changed to 10.00:EUR or an amount of
 * 10.00:EUR is added, if missing.
 */
class ExtractionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExtractionsBinding

    private var extractions: MutableMap<String, GiniCaptureSpecificExtraction> = hashMapOf()
    private lateinit var extractionsAdapter: ExtractionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtractionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readExtras()
        setUpRecyclerView(binding)
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
                getParcelable<GiniCaptureSpecificExtraction>(name)?.let { extractions[name] = it }
            }
        }
    }

    private fun setUpRecyclerView(binding: ActivityExtractionsBinding) {
        binding.recyclerviewExtractions.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ExtractionsActivity)
            adapter = ExtractionsAdapter(getSortedExtractions(extractions)).also {
                extractionsAdapter = it
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> = extractions.toSortedMap().values.toList()

    private fun sendFeedback(binding: ActivityExtractionsBinding) {
        // An example for sending feedback where we change the amount or add one if it is missing
        // Feedback should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        val amount = extractions["amountToPay"]
        if (amount != null) { // Let's assume the amount was wrong and change it
            amount.value = "10.00:EUR"
            Toast.makeText(this, "Amount changed to 10.00:EUR", Toast.LENGTH_SHORT).show()
        } else { // Amount was missing, let's add it
            extractions["amountToPay"] = GiniCaptureSpecificExtraction("amountToPay", "10.00:EUR", "amount", null, emptyList())
            extractionsAdapter.extractions = getSortedExtractions(extractions)
            Toast.makeText(this, "Added amount of 10.00:EUR", Toast.LENGTH_SHORT).show()
        }
        extractionsAdapter.notifyDataSetChanged()
        showProgressIndicator(binding)
        val giniCaptureNetworkApi = GiniCapture.getInstance().giniCaptureNetworkApi
        if (giniCaptureNetworkApi == null) {
            Toast.makeText(
                this, "Feedback not sent: missing GiniCaptureNetworkApi implementation.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        giniCaptureNetworkApi.sendFeedback(extractions, object : GiniCaptureNetworkCallback<Void, Error> {
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
        private const val EXTRA_IN_COMPOUND_EXTRACTIONS = "EXTRA_IN_COMPOUND_EXTRACTIONS"
        private const val EXTRA_IN_EXTRACTIONS = "EXTRA_IN_EXTRACTIONS"

        fun getStartIntent(
            context: Context,
            extractions: Map<String, GiniCaptureSpecificExtraction>,
            compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap()
        ): Intent =
            Intent(context, ExtractionsActivity::class.java).apply {
                putExtra(EXTRA_IN_EXTRACTIONS, Bundle().apply {
                    extractions.map { putParcelable(it.key, it.value) }
                })
                putExtra(EXTRA_IN_COMPOUND_EXTRACTIONS, Bundle().apply {
                    compoundExtractions.map { putParcelable(it.key, it.value) }
                })
            }
    }
}

private class ExtractionsAdapter(var extractions: List<GiniCaptureSpecificExtraction>) : RecyclerView.Adapter<ExtractionsAdapter.ExtractionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtractionsViewHolder =
        ExtractionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_extraction, parent, false))

    override fun onBindViewHolder(holder: ExtractionsViewHolder, position: Int) {
        extractions.getOrNull(position)?.run {
            holder.textName.text = name
            holder.textValue.text = value
        }
    }

    override fun getItemCount(): Int = extractions.size

    private class ExtractionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textName: TextView = itemView.findViewById<View>(R.id.text_name) as TextView
        var textValue: TextView = itemView.findViewById<View>(R.id.text_value) as TextView
    }
}