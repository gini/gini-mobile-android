package net.gini.pay.appcomponentapi.digitalinvoice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import java.util.*
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.pay.appcomponentapi.R
import net.gini.pay.appcomponentapi.databinding.ActivityLineItemDetailsBinding
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsFragmentListener

/**
 * Created by Alpar Szotyori on 08.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
class LineItemDetailsExampleActivity : AppCompatActivity(), LineItemDetailsFragmentListener, LineItemDetailsContract.Contract {

    private var lineItemDetailsFragment: LineItemDetailsFragment? = null
    private lateinit var input: LineItemDetailsInput

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLineItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBar(binding.toolbar)
        setTitles()
        readExtras()
        if (savedInstanceState == null) {
            createLineItemDetailsFragment()
            showLineItemDetailsFragment()
        } else {
            retrieveLineItemDetailsFragment()
        }
    }

    override fun onSave(selectableLineItem: SelectableLineItem) {
        setResult(RESULT_OK, Intent().setResult(selectableLineItem))
        finish()
    }

    private fun readExtras() {
        input = intent.getInput()
    }

    private fun setUpActionBar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setTitles() {
        supportActionBar?.run {
            title = getString(R.string.line_item_details_screen_title)
            subtitle = getString(R.string.line_item_details_screen_subtitle)
        }
    }

    private fun createLineItemDetailsFragment() {
        lineItemDetailsFragment = LineItemDetailsFragment.createInstance(input.item, input.returnReasons)
    }

    private fun showLineItemDetailsFragment() {
        supportFragmentManager.commit {
            replace(R.id.line_item_details_screen_container, lineItemDetailsFragment as Fragment)
        }
    }

    private fun retrieveLineItemDetailsFragment() {
        lineItemDetailsFragment = supportFragmentManager.findFragmentById(
            R.id.line_item_details_screen_container
        ) as LineItemDetailsFragment?
    }

}

data class LineItemDetailsInput(
    val item: SelectableLineItem,
    val returnReasons: List<GiniCaptureReturnReason>,
)

class LineItemDetailsContract : ActivityResultContract<LineItemDetailsInput, SelectableLineItem>() {
    override fun createIntent(context: Context, input: LineItemDetailsInput) =
        Intent(context, LineItemDetailsExampleActivity::class.java).apply {
            putExtra(EXTRA_IN_SELECTABLE_LINE_ITEM, input.item)
            putParcelableArrayListExtra(EXTRA_IN_RETURN_REASONS, ArrayList<Parcelable>(input.returnReasons))
        }

    override fun parseResult(resultCode: Int, result: Intent?): SelectableLineItem? {
        if (resultCode == Activity.RESULT_OK) {
            return result?.getParcelableExtra(EXTRA_OUT_SELECTABLE_LINE_ITEM)
        }
        return null
    }

    companion object {
        private const val EXTRA_IN_SELECTABLE_LINE_ITEM = "EXTRA_IN_SELECTABLE_LINE_ITEM"
        private const val EXTRA_IN_RETURN_REASONS = "EXTRA_IN_RETURN_REASONS"
        const val EXTRA_OUT_SELECTABLE_LINE_ITEM = "EXTRA_OUT_SELECTABLE_LINE_ITEM"
    }

    interface Contract {
        fun Intent.getInput() = LineItemDetailsInput(
            getParcelableExtra(EXTRA_IN_SELECTABLE_LINE_ITEM)!!,
            getParcelableArrayListExtra(EXTRA_IN_RETURN_REASONS)!!
        )

        fun Intent.setResult(item: SelectableLineItem) = this.apply {
            putExtra(EXTRA_OUT_SELECTABLE_LINE_ITEM, item)
        }
    }
}