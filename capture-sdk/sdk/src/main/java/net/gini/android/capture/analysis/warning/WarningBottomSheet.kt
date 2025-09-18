package net.gini.android.capture.analysis.warning

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.capture.R
import net.gini.android.capture.databinding.GcWarningBottomSheetBinding
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme


class WarningBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onCancelAction()
        fun onProceedAction()
    }

    private lateinit var binding: GcWarningBottomSheetBinding

    private var titleText: CharSequence? = null
    private var descText: CharSequence? = null

    var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = arguments?.getSerializable(ARG_TYPE) as? WarningType
        if (type != null) {
            titleText = getString(type.titleRes)
            descText  = getString(type.descriptionRes)
        } else {
            // Fallback if nothing passed
            titleText = arguments?.getCharSequence(ARG_TITLE)
            descText  = arguments?.getCharSequence(ARG_DESC)
        }
    }


    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            activity?.let {
                binding = GcWarningBottomSheetBinding.inflate(getLayoutInflaterWithGiniCaptureTheme(it.layoutInflater), null, false)

                val builder = AlertDialog.Builder(context)
                builder.setView(binding.root)
                bindUi()

                val alertDialog = builder.create()
                return alertDialog
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun getTheme(): Int {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            return super.getTheme()
        }
        return super.getTheme()
        // Use your app theme for bottom sheets
//        return R.style.GiniCaptureTheme_BottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        binding = GcWarningBottomSheetBinding.inflate(inflater, container, false)
        handleBottomSheetConfigurations()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.gc_warning_title_dialog))
        bindUi()
    }
    private fun handleBottomSheetConfigurations() {
        dialog?.setOnShowListener {
            val bottomSheetInternal =
                (it as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            (it as? BottomSheetDialog)?.setCanceledOnTouchOutside(false)

            bottomSheetInternal?.let {
                BottomSheetBehavior.from(bottomSheetInternal).apply {
                    isDraggable = false
                    isHideable = false
                    state = BottomSheetBehavior.STATE_EXPANDED
                    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            if (newState in listOf(
                                    BottomSheetBehavior.STATE_HIDDEN,
                                    STATE_COLLAPSED
                                )
                            )
                                dismiss()

                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                    })
                }
            }
        }
    }
    private fun bindUi() {
        binding.warningTitle.text = titleText
        binding.warningDescription.text = descText
        // Icon is decorative only
        binding.warningIcon?.contentDescription = null
        binding.warningIcon?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        binding.primaryButton.setOnClickListener {
            listener?.onCancelAction()
            dismissAllowingStateLoss()
        }
        binding.secondaryButton.setOnClickListener {
            listener?.onProceedAction()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_TYPE  = "arg_type"
        private const val ARG_TITLE = "arg_title" // kept for backward-compat if needed
        private const val ARG_DESC  = "arg_desc"

        @JvmStatic
        fun newInstance(type: WarningType) = WarningBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TYPE, type) // WarningType is an enum (Serializable)
            }
        }
    }
}