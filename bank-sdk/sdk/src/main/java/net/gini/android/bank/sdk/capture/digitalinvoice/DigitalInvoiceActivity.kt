package net.gini.android.bank.sdk.capture.digitalinvoice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsListener
import net.gini.android.bank.sdk.capture.digitalinvoice.info.DigitalInvoiceInfoFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.info.DigitalInvoiceInfoFragmentListener
import net.gini.android.bank.sdk.capture.digitalinvoice.onboarding.DigitalInvoiceOnboardingFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.onboarding.DigitalInvoiceOnboardingFragmentListener
import net.gini.android.bank.sdk.capture.internalParseResult
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

private const val RETURN_ASSISTANT_FRAGMENT = "RETURN_ASSISTANT_FRAGMENT"

private const val EXTRA_IN_EXTRACTIONS = "EXTRA_IN_EXTRACTIONS"

private const val EXTRA_IN_COMPOUND_EXTRACTIONS = "EXTRA_IN_COMPOUND_EXTRACTIONS"

private const val EXTRA_IN_RETURN_REASONS = "EXTRA_IN_RETURN_REASONS"
private const val TAG_ONBOARDING = "TAG_ONBOARDING"
private const val TAG_INFO = "TAG_INFO"

/**
 * Internal use only.
 *
 * When you use the Screen API, the `DigitalInvoiceActivity` displays the line items extracted from an invoice document and their total
 * price. The user can deselect line items which should not be paid for and also edit the quantity, price or description of each line item.
 * The total price is always updated to include only the selected line items.
 *
 * The returned extractions in the [CameraActivity.EXTRA_OUT_EXTRACTIONS] and [CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS] are updated to
 * include the user's modifications:
 * - "amountToPay" is updated to contain the sum of the selected line items' prices,
 * - the line items are updated according to the user's modifications.
 *
 * The `DigitalInvoiceActivity` is started if the following are true:
 * - analysis completed successfully
 * - line item extractions have been enabled for your client id
 * - the analysis result contains line item extractions
 *
 * ### Customizing the Digital Invoice Screen
 *
 * Customizing the look of the Digital Invoice Screen is done via overriding of app resources.
 *
 * **Important:** All overriden styles must have their respective `Root.` prefixed style as their parent. Ex.: the parent of
 * `GiniCaptureTheme.Snackbar.Error.TextStyle` must be `Root.GiniCaptureTheme.Snackbar.Error.TextStyle`.
 */
internal class DigitalInvoiceActivity : AppCompatActivity(), DigitalInvoiceFragmentListener,
    DigitalInvoiceInfoFragmentListener, DigitalInvoiceOnboardingFragmentListener, LineItemDetailsListener {

    private var fragment: DigitalInvoiceFragment? = null
    private lateinit var extractions: Map<String, GiniCaptureSpecificExtraction>
    private lateinit var compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    private lateinit var returnReasons: List<GiniCaptureReturnReason>

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gbs_activity_digital_invoice)
        readExtras()
        if (savedInstanceState == null) {
            initFragment()
        } else {
            retainFragment()
        }
        enableHomeAsUp(this)

        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }


    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        if (item.itemId == R.id.help) {
            showInfo()
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.gbs_menu_digital_invoice, menu)
        return true
    }

    private fun readExtras() {
        extractions = intent.extras?.getBundle(EXTRA_IN_EXTRACTIONS)?.run {
            keySet().map { it to getParcelable<GiniCaptureSpecificExtraction>(it)!! }.toMap()
        } ?: emptyMap()
        compoundExtractions = intent.extras?.getBundle(EXTRA_IN_COMPOUND_EXTRACTIONS)?.run {
            keySet().map { it to getParcelable<GiniCaptureCompoundExtraction>(it)!! }.toMap()
        } ?: emptyMap()
        returnReasons =
            intent.extras?.getParcelableArrayList(EXTRA_IN_RETURN_REASONS) ?: emptyList()
    }

    private fun initFragment() {
        if (!isFragmentShown()) {
            createFragment()
            showFragment()
        }
    }

    private fun isFragmentShown() = supportFragmentManager.findFragmentByTag(
        RETURN_ASSISTANT_FRAGMENT
    ) != null

    private fun createFragment() {
        fragment =
            DigitalInvoiceFragment.createInstance(extractions, compoundExtractions, returnReasons)
    }

    private fun showFragment() = fragment?.let { digitalInvoiceFragment ->
        supportFragmentManager.commit {
            add(
                R.id.fragment_digital_invoice,
                digitalInvoiceFragment,
                RETURN_ASSISTANT_FRAGMENT
            )
        }
    }

    private fun retainFragment() {
        fragment = supportFragmentManager.findFragmentByTag(
            RETURN_ASSISTANT_FRAGMENT
        ) as DigitalInvoiceFragment?
    }

    private fun showInfo() {
        if (supportFragmentManager.findFragmentByTag(TAG_INFO) != null) {
            return
        }
        supportFragmentManager.commit {
            val infoFragment = DigitalInvoiceInfoFragment.createInstance().apply {
                listener = this@DigitalInvoiceActivity
            }
            add(R.id.fragment_digital_invoice, infoFragment, TAG_INFO)
        }
    }

    override fun onCloseInfo() {
        (supportFragmentManager.findFragmentByTag(TAG_INFO) as? DigitalInvoiceInfoFragment)?.let { infoFragment ->
            infoFragment.listener = null
            supportFragmentManager.commit {
                remove(infoFragment)
            }
        }
    }

    override fun showOnboarding() {
        supportFragmentManager.commit {
            val onboardingFragment = DigitalInvoiceOnboardingFragment.createInstance().apply {
                listener = this@DigitalInvoiceActivity
            }
            add(R.id.fragment_digital_invoice, onboardingFragment, TAG_ONBOARDING)
        }
    }

    override fun onCloseOnboarding() {
        (supportFragmentManager.findFragmentByTag(TAG_ONBOARDING) as? DigitalInvoiceOnboardingFragment)?.let { infoFragment ->
            infoFragment.listener = null
            supportFragmentManager.commit {
                remove(infoFragment)
            }
        }
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onEditLineItem(selectableLineItem: SelectableLineItem) {
        val bottomSheet = DigitalInvoiceBottomSheet.newInstance(selectableLineItem)
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    fun resultFromBottomSheet(selectableLineItem: SelectableLineItem) {
        fragment?.updateLineItem(selectableLineItem)
    }

    override fun onAddLineItem(selectableLineItem: SelectableLineItem) {}

    /**
     * Internal use only.
     *
     * @suppress
     */
    override fun onPayInvoice(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    ) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, Bundle().apply {
                specificExtractions.forEach { putParcelable(it.key, it.value) }
            })
            putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, Bundle().apply {
                compoundExtractions.forEach { putParcelable(it.key, it.value) }
            })
        })
        finish()
    }

    override fun onSave(selectableLineItem: SelectableLineItem) {
        fragment?.updateLineItem(selectableLineItem)
    }
}

internal data class DigitalInvoiceInput(
    val extractions: Map<String, GiniCaptureSpecificExtraction>,
    val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    val returnReasons: List<GiniCaptureReturnReason>,
)

internal fun CaptureResult.Success.toDigitalInvoiceInput() = DigitalInvoiceInput(
    specificExtractions, compoundExtractions, returnReasons
)

internal class DigitalInvoiceContract :
    ActivityResultContract<DigitalInvoiceInput, CaptureResult>() {
    override fun createIntent(context: Context, input: DigitalInvoiceInput) =
        Intent(context, DigitalInvoiceActivity::class.java).apply {
            putExtra(EXTRA_IN_EXTRACTIONS, Bundle().apply {
                input.extractions.forEach { putParcelable(it.key, it.value) }
            })
            putExtra(EXTRA_IN_COMPOUND_EXTRACTIONS, Bundle().apply {
                input.compoundExtractions.forEach { putParcelable(it.key, it.value) }
            })
            putParcelableArrayListExtra(EXTRA_IN_RETURN_REASONS, ArrayList(input.returnReasons))
        }

    override fun parseResult(resultCode: Int, result: Intent?): CaptureResult {
        return internalParseResult(resultCode, result)
    }
}