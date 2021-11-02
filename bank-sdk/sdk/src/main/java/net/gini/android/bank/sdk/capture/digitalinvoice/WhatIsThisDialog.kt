package net.gini.android.bank.sdk.capture.digitalinvoice

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentWhatIsThisDialogBinding

/**
 * Created by Alpar Szotyori on 24.01.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

internal typealias WhatIsThisDialogResultCallback = (Boolean?) -> Unit

/**
 * Internal use only.
 *
 * @suppress
 */
internal class WhatIsThisDialog : BottomSheetDialogFragment() {

    private var binding by autoCleared<GbsFragmentWhatIsThisDialogBinding>()
    var callback: WhatIsThisDialogResultCallback? = null

    override fun getTheme(): Int = R.style.GiniCaptureTheme_BottomSheetDialog

    companion object {
        @JvmStatic
        fun createInstance() = WhatIsThisDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = GbsFragmentWhatIsThisDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListView()
    }

    private fun initListView() {
        activity?.let {
            val responses = listOf(
                resources.getString(R.string.gbs_digital_invoice_what_is_this_dialog_positive_response),
                resources.getString(R.string.gbs_digital_invoice_what_is_this_dialog_negative_response)
            )
            binding.gbsDigitalInvoiceWhatIsThisDialogResponses.adapter =
                ArrayAdapter(it, R.layout.gbs_item_digital_invoice_what_is_this_dialog_response, responses)
            binding.gbsDigitalInvoiceWhatIsThisDialogResponses.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                callback?.invoke(position == 0)
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        callback?.invoke(null)
    }
}