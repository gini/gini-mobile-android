package net.gini.android.capture.screen.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.databinding.ActivityExtractionsBinding
import net.gini.android.core.api.models.SpecificExtraction
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

/**
 * Displays the Pay5 extractions: paymentRecipient, iban, bic, amount and paymentReference.
 *
 * A menu item is added to send feedback.
 */

@AndroidEntryPoint
class ExtractionsActivity : AppCompatActivity(),
    ExtractionsAdapterImpl.ExtractionsAdapterInterface {
    private lateinit var binding: ActivityExtractionsBinding

    private var mExtractions: MutableMap<String, GiniCaptureSpecificExtraction> = HashMap()
    private var mCompoundExtractions: Map<String, GiniCaptureCompoundExtraction> = HashMap()
    private val mLegacyExtractions: MutableMap<String, SpecificExtraction> = HashMap()
    private var mExtractionsAdapter: ExtractionsAdapter<Any>? = null
    @Inject lateinit var giniCaptureDefaultNetworkService: GiniCaptureDefaultNetworkService

    // {extraction name} to it's {entity name}
    private val editableSpecificExtractions = hashMapOf("paymentRecipient" to "companyname", "paymentReference" to "reference" ,
        "paymentPurpose" to "text", "iban" to "iban", "bic" to "bic", "amountToPay" to "amount")

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
        showAnalyzedDocumentId()
        setUpRecyclerView(binding)
    }

    override fun valueChanged(key: String, newValue: String) {
        mExtractions[key]?.apply {
            value = newValue
        }
    }

    private fun showAnalyzedDocumentId() {
        val documentId = giniCaptureDefaultNetworkService.analyzedGiniApiDocument?.id ?: ""
        binding.textDocumentId.text = getString(R.string.analyzed_document_id, documentId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_extractions, menu)
        menu.findItem(R.id.view_picture)?.let {
            it.isEnabled = analyzedCameraPicture != null
            it.isVisible = it.isEnabled
        }
        return true
    }

    private val analyzedCameraPicture: ByteArray?
        get() {
            // GiniCaptureAccountingNetworkService was removed
            return null
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.feedback -> {
                sendFeedbackAndClose(binding)
                true
            }
            R.id.view_picture -> {
                viewPicture()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun viewPicture() {
        savePictureToFile()?.let { file ->
            try {
                FileProvider.getUriForFile(
                        this,
                        "net.gini.android.capture.screen.fileprovider",
                        file)
            } catch (e: Exception) {
                LOG.error("Error sharing the pictue {} ", file.absolutePath, e)
                Toast.makeText(this,
                        "Error sharing the picture {} " + file.absolutePath,
                        Toast.LENGTH_LONG).show()
                return
            }?.let { fileUri ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(fileUri, contentResolver.getType(fileUri))
                if (packageManager.queryIntentActivities(intent, 0).isEmpty()) {
                    Toast.makeText(this, "No image viewer app found",
                            Toast.LENGTH_LONG).show()
                } else {
                    startActivity(intent)
                }
            }
        } ?: run {
            Toast.makeText(this, "Could not write picture to file",
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun savePictureToFile(): File? =
            analyzedCameraPicture?.let { picture ->
                val jpegFilename = "${Date().time}.jpeg"
                createPictureDir()?.let { picDir ->
                    val jpegFile = File(picDir, jpegFilename)
                    var fileOutputStream: FileOutputStream? = null
                    try {
                        fileOutputStream = FileOutputStream(jpegFile).apply {
                            write(picture, 0, picture.size)
                        }
                        LOG.debug("Picture written to {}", jpegFile.absolutePath)
                        jpegFile
                    } catch (e: IOException) {
                        LOG.error("Failed to save picture to {}",
                                jpegFile.absolutePath, e)
                        null
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close()
                            } catch (e: IOException) {
                                LOG.error("Closing FileOutputStream failed for {}", jpegFile.absolutePath, e)
                            }
                        }
                    }
                }
            }

    private fun createPictureDir(): File? {
        val externalFilesDir = getExternalFilesDir(null)
        val pictureDir = File(externalFilesDir, "camera-pictures")
        return if (pictureDir.exists() || pictureDir.mkdir()) {
            pictureDir
        } else null
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
            
            editableSpecificExtractions.forEach {
                if (!mExtractions.containsKey(it.key)) {
                    mExtractions[it.key] = GiniCaptureSpecificExtraction(
                        it.key, "",
                        it.value, null, emptyList())
                }
            }

            adapter = when {
                mExtractions.isNotEmpty() -> ExtractionsAdapterImpl(getSortedExtractions(mExtractions),
                        getSortedExtractions(mCompoundExtractions), editableSpecificExtractions.keys.toList(), this@ExtractionsActivity)
                mLegacyExtractions.isNotEmpty() -> LegacyExtractionsAdapter(getSortedExtractions(mLegacyExtractions))
                else -> null
            }
            setOnTouchListener { _, _ ->
                performClick()
                val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    private fun <T> getSortedExtractions(extractions: Map<String, T>): List<T> = extractions.toSortedMap().values.toList()

    private fun sendFeedbackAndClose(binding: ActivityExtractionsBinding) {
        // Feedback should be sent only for the user visible fields. Non-visible fields should be filtered out.
        // In a real application the user input should be used as the new value.

        var amount = mExtractions["amountToPay"]?.value ?: ""
        val paymentRecipient = mExtractions["paymentRecipient"]?.value ?: ""
        val paymentReference = mExtractions["paymentReference"]?.value ?: ""
        val paymentPurpose = mExtractions["paymentPurpose"]?.value ?: ""
        val iban = mExtractions["iban"]?.value ?: ""
        val bic = mExtractions["bic"]?.value ?: ""

        if (amount.isEmpty()) {
            amount = Amount.EMPTY.amountToPay()
            mExtractions["amountToPay"]?.value = amount
            mExtractionsAdapter?.extractions = getSortedExtractions(mExtractions)
        }
        mExtractionsAdapter?.notifyDataSetChanged()

        GiniCapture.cleanup(applicationContext, paymentRecipient, paymentReference, paymentPurpose, iban, bic, Amount(
            BigDecimal(amount.removeSuffix(":EUR")), AmountCurrency.EUR)
        )

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
}

private abstract class ExtractionsAdapter<T> :
    RecyclerView.Adapter<ExtractionsViewHolder>() {
    abstract var extractions: List<T>
    abstract var compoundExtractions: List<GiniCaptureCompoundExtraction>
    abstract var editableSpecificExtractions : List<String>
}

private class ExtractionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var mTextInputLayout: TextInputLayout = itemView.findViewById(R.id.text_input_layout)
    var mTextValue: TextInputEditText = itemView.findViewById(R.id.text_value)
}

private class ExtractionsAdapterImpl(
    override var extractions: List<GiniCaptureSpecificExtraction>,
    override var compoundExtractions: List<GiniCaptureCompoundExtraction>,
    override var editableSpecificExtractions: List<String>,
    var listener: ExtractionsAdapterInterface?
) : ExtractionsAdapter<GiniCaptureSpecificExtraction>() {

    interface ExtractionsAdapterInterface {
        fun valueChanged(key: String, value: String)
    }
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ExtractionsViewHolder {
        val holder = ExtractionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_extraction, parent, false))
        holder.mTextValue.addTextChangedListener {
            listener?.valueChanged(holder.mTextInputLayout.hint.toString(), it.toString())
        }

        return holder
    }

    override fun onBindViewHolder(holder: ExtractionsViewHolder,
                                  position: Int) {
        extractions.getOrNull(position)?.run {
            holder.mTextValue.setText(value)
            holder.mTextInputLayout.hint = name
            holder.mTextValue.isEnabled = name in editableSpecificExtractions
        } ?: compoundExtractions.getOrNull(position - extractions.size)?.run {
            holder.mTextInputLayout.hint = name
            holder.mTextValue.isEnabled = false
            holder.mTextValue.setText(StringBuilder().apply {
                specificExtractionMaps.forEach { extractionMap ->
                    extractionMap.forEach { (name, extraction) ->
                        append("${name}: ${extraction.value}\n")
                    }
                    append("\n")
                }
            })
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
    override var editableSpecificExtractions: List<String> = listOf()
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ExtractionsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ExtractionsViewHolder(
            layoutInflater.inflate(R.layout.item_extraction, parent, false))
    }

    override fun onBindViewHolder(holder: ExtractionsViewHolder,
                                  position: Int) {
        holder.mTextInputLayout.hint = extractions[position].name
        holder.mTextValue.setText(extractions[position].value)
    }

    override fun getItemCount(): Int {
        return extractions.size
    }
}
