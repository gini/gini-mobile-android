package net.gini.android.bank.sdk.capture.digitalinvoice

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.util.autoCleared
import net.gini.android.bank.sdk.databinding.GbsFragmentReturnReasonDialogBinding
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.bank.sdk.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.internal.util.ContextHelper

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

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
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
        handleCloseButton()
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            dialog?.window?.disallowScreenshots()
        }
    }

    private fun handleCloseButton() {
        if (isPhoneLandscapeMode()) {
            binding.gbsCloseBottomSheet?.visibility = View.VISIBLE
            binding.gbsCloseBottomSheet?.setIntervalClickListener {
                dismiss()
            }
        }
    }

    private fun isPhoneLandscapeMode() = !ContextHelper.isPortraitOrTablet(requireContext())

    private fun initListView() {
        activity?.let {
            binding.gbsReturnReasonsList.adapter = ArrayAdapter(it, R.layout.gbs_item_return_reason, localizedReasons())
            binding.gbsReturnReasonsList.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    callback?.invoke(reasons[position])
                    dismiss()
                }
        }
    }

    private fun localizedReasons() = reasons.map { it.labelInLocalLanguageOrGerman ?: "" }

    override fun onCancel(dialog: DialogInterface) {
        callback?.invoke(null)
    }

    override fun onStart() {
        super.onStart()
        view?.let {
            val mBehavior = BottomSheetBehavior.from(it.parent as View)
            mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (ContextHelper.isTablet(requireContext())) {
                mBehavior.maxWidth =
                    resources.getDimension(net.gini.android.capture.R.dimen.gc_tablet_width).toInt()
            } else if (isPhoneLandscapeMode()) {
                dialog?.window?.let { window ->
                    val (screenWidth, screenHeight) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val metrics = window.windowManager.currentWindowMetrics
                        val bounds = metrics.bounds
                        bounds.width() to bounds.height()
                    } else {
                        val displayMetrics = DisplayMetrics()
                        @Suppress("DEPRECATION")
                        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
                        displayMetrics.widthPixels to displayMetrics.heightPixels
                    }

                    window.setLayout(screenWidth / 2, screenHeight)
                    val params = window.attributes
                    params.gravity = Gravity.END
                    window.attributes = params
                    mBehavior.isDraggable = false
                }
            }
        }
    }
}