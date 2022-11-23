package net.gini.android.capture.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.component.databinding.ActivityExtractionsBinding
import net.gini.android.capture.example.shared.BaseExampleApp
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONException
import org.slf4j.LoggerFactory

/**
 * <p>
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 * </p>
 * <p>
 * A menu item is added to send feedback. The amount is changed to 10.00:EUR or an amount of
 * 10.00:EUR is added, if missing.
 * </p>
 */
class ExtractionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = HashMap()
    private var mCompoundExtractions: Map<String, GiniCaptureCompoundExtraction> = HashMap()
    private val mLegacyExtractions: MutableMap<String, SpecificExtraction> = HashMap()
    private var mExtractionsAdapter: ExtractionsAdapter<Any>? = null

    companion object {
        private val LOG = LoggerFactory.getLogger(ExtractionsActivity::class.java)
        const val EXTRA_IN_EXTRACTIONS = "EXTRA_IN_EXTRACTIONS"
        const val EXTRA_IN_COMPOUND_EXTRACTIONS = "EXTRA_IN_COMPOUND_EXTRACTIONS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtractionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readExtras()
        setUpRecyclerView(binding)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.feedback -> {
                sendFeedback(binding)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun readExtras() {
        intent.extras?.getParcelable<Bundle>(EXTRA_IN_EXTRACTIONS)?.run {
            keySet().forEach { name ->
                try {
                    mExtractions[name] = getParcelable(name)!!
                } catch (e: ClassCastException) {
                    mLegacyExtractions[name] = getParcelable(name)!!
                }
            }
        }
        mCompoundExtractions = intent.extras?.getParcelable<Bundle>(EXTRA_IN_COMPOUND_EXTRACTIONS)?.run {
            keySet().map { it to getParcelable<GiniCaptureCompoundExtraction>(it)!! }.toMap()
        } ?: emptyMap()
    }

    private fun setUpRecyclerView(binding: ActivityExtractionsBinding) {
        binding.recyclerviewExtractions.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ExtractionsActivity)
            adapter = when {
                mExtractions.isNotEmpty() -> ExtractionsAdapterImpl(getSortedExtractions(mExtractions), getSortedExtractions(mCompoundExtractions))
                mLegacyExtractions.isNotEmpty() -> LegacyExtractionsAdapter(getSortedExtractions(mLegacyExtractions))
                else -> null
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
            val extraction = GiniCaptureSpecificExtraction(
                    "amountToPay", "10.00:EUR",
                    "amount", null, emptyList())
            mExtractions["amountToPay"] = extraction
            mExtractionsAdapter?.extractions = getSortedExtractions(mExtractions)
            Toast.makeText(this, "Added amount of 10.00:EUR", Toast.LENGTH_SHORT).show()
        }
        mExtractionsAdapter?.notifyDataSetChanged()
        showProgressIndicator(binding)
        val giniCaptureNetworkApi = GiniCapture.getInstance().giniCaptureNetworkApi
        if (giniCaptureNetworkApi == null) {
            Toast.makeText(this, "Feedback not sent: missing GiniCaptureNetworkApi implementation.",
                    Toast.LENGTH_SHORT).show()
            return
        }
        giniCaptureNetworkApi.sendFeedback(mExtractions, object : GiniCaptureNetworkCallback<Void, Error> {
            override fun failure(error: Error) {
                hideProgressIndicator(binding)
                Toast.makeText(this@ExtractionsActivity,
                        "Feedback error:\n" + error.message,
                        Toast.LENGTH_LONG).show()
            }

            override fun success(result: Void) {
                hideProgressIndicator(binding)
                Toast.makeText(this@ExtractionsActivity,
                        "Feedback successful",
                        Toast.LENGTH_LONG).show()
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

    private abstract class ExtractionsAdapter<T> :
            RecyclerView.Adapter<ExtractionsViewHolder>() {
        abstract var extractions: List<T>
        abstract var compoundExtractions: List<GiniCaptureCompoundExtraction>
    }

    private class ExtractionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mTextName: TextView
        var mTextValue: TextView

        init {
            mTextName = itemView.findViewById<View>(R.id.text_name) as TextView
            mTextValue = itemView.findViewById<View>(R.id.text_value) as TextView
        }
    }

    private class ExtractionsAdapterImpl(
            override var extractions: List<GiniCaptureSpecificExtraction>,
            override var compoundExtractions: List<GiniCaptureCompoundExtraction>
    ) : ExtractionsAdapter<GiniCaptureSpecificExtraction>() {

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): ExtractionsViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return ExtractionsViewHolder(
                    layoutInflater.inflate(R.layout.item_extraction, parent, false))
        }

        override fun onBindViewHolder(holder: ExtractionsViewHolder,
                                      position: Int) {
            extractions.getOrNull(position)?.run {
                holder.mTextName.text = name
                holder.mTextValue.text = value
            } ?: compoundExtractions.getOrNull(position - extractions.size)?.run {
                holder.mTextName.text = name
                holder.mTextValue.text = StringBuilder().apply {
                    specificExtractionMaps.forEach { extractionMap ->
                        extractionMap.forEach { (name, extraction) ->
                            append("${name}: ${extraction.value}\n")
                        }
                        append("\n")
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return extractions.size + compoundExtractions.size
        }

    }

    private class LegacyExtractionsAdapter(
            override var extractions: List<SpecificExtraction>,
            override var compoundExtractions: List<GiniCaptureCompoundExtraction> = emptyList()
    ) : ExtractionsAdapter<SpecificExtraction>() {

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): ExtractionsViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return ExtractionsViewHolder(
                    layoutInflater.inflate(R.layout.item_extraction, parent, false))
        }

        override fun onBindViewHolder(holder: ExtractionsViewHolder,
                                      position: Int) {
            holder.mTextName.text = extractions[position].name
            holder.mTextValue.text = extractions[position].value
        }

        override fun getItemCount(): Int {
            return extractions.size
        }

    }
}