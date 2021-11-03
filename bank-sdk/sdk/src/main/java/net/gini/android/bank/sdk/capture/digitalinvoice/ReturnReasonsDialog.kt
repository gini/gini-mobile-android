package net.gini.android.bank.sdk.capture.digitalinvoice

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.collections.ArrayList
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentReturnReasonDialogBinding

/**
 * Created by Alpar Szotyori on 22.01.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

private const val ARG_RETURN_REASONS = "GBS_ARG_SELECTABLE_LINE_ITEM"

internal typealias ReturnReasonDialogResultCallback = (GiniCaptureReturnReason?) -> Unit

/**
 * Internal use only.
 *
 * @suppress
 */
internal class ReturnReasonDialog : BottomSheetDialogFragment() {

    private var binding by autoCleared<GbsFragmentReturnReasonDialogBinding>()
    private lateinit var reasons: List<GiniCaptureReturnReason>

    var callback: ReturnReasonDialogResultCallback? = null

    override fun getTheme(): Int = R.style.GiniCaptureTheme_BottomSheetDialog

    companion object {
        @JvmStatic
        fun createInstance(reasons: List<GiniCaptureReturnReason>) = ReturnReasonDialog().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_RETURN_REASONS, ArrayList(reasons))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readArguments()
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    private fun readArguments() {
        arguments?.run {
            reasons = getParcelableArrayList(ARG_RETURN_REASONS) ?: emptyList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = GbsFragmentReturnReasonDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListView()
    }

    private fun initListView() {
        activity?.let {
            binding.gbsReturnReasonsList.adapter = ArrayAdapter(it, R.layout.gbs_item_return_reason, localizedReasons())
            binding.gbsReturnReasonsList.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    callback?.invoke(reasons[position])
                    dismissAllowingStateLoss()
                }
        }
    }

    private fun localizedReasons() = reasons.map { it.labelInLocalLanguageOrGerman ?: "" }

    override fun onCancel(dialog: DialogInterface) {
        callback?.invoke(null)
    }
}