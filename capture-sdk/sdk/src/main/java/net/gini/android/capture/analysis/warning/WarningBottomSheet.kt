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

/**
 * A reusable bottom sheet (or dialog on tablets) that displays a warning message.
 *
 * Responsibilities:
 * - Presents warning information to the user in a modal sheet/dialog.
 * - Provides two actions: cancel (primary button) and proceed (secondary button).
 * - Handles accessibility (content descriptions, pane title).
 * - Adapts its layout and behavior for phones (BottomSheetDialog) and tablets (AlertDialog).
 *
 * Use [newInstance] with a [WarningType] to configure the title and description.
 */

class WarningBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onCancelAction()
        fun onProceedAction()
    }

    private lateinit var binding: GcWarningBottomSheetBinding

    private var titleText: CharSequence? = null
    private var descText: CharSequence? = null

    var listener: Listener? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = arguments?.getSerializable(ARG_TYPE) as? WarningType
        if (type != null) {
            titleText = getString(type.titleRes)
            descText = getString(type.descriptionRes)
        }
    }


    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (resources.getBoolean(R.bool.gc_is_tablet)) {
            activity?.let {
                binding = GcWarningBottomSheetBinding.inflate(
                    getLayoutInflaterWithGiniCaptureTheme(it.layoutInflater),
                    null,
                    false
                )

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
        return R.style.GiniCaptureTheme_BottomSheetDialog
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
        dialog?.setOnShowListener { dlg ->
            val bottomSheetInternal = (dlg as? BottomSheetDialog)
                ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            dlg.setCanceledOnTouchOutside(false)

            this.isCancelable = false

            bottomSheetInternal.let {
                BottomSheetBehavior.from(bottomSheetInternal).apply {
                    isDraggable = false
                    isHideable = false
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bs: View, newState: Int) {
                            when (newState) {
                                BottomSheetBehavior.STATE_DRAGGING,
                                BottomSheetBehavior.STATE_SETTLING,
                                BottomSheetBehavior.STATE_COLLAPSED -> {
                                    state = BottomSheetBehavior.STATE_EXPANDED
                                }
                                BottomSheetBehavior.STATE_HIDDEN -> {
                                    state = BottomSheetBehavior.STATE_EXPANDED
                                }
                                else -> Unit
                            }
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

        binding.warningIcon?.contentDescription = getString(R.string.warning_icon_description)
        binding.warningIcon?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        binding.cancelButton.setOnClickListener {
            listener?.onCancelAction()
            dismissAllowingStateLoss()
        }
        binding.proceedButton.setOnClickListener {
            listener?.onProceedAction()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_TYPE = "arg_type"
        @JvmStatic
        fun newInstance(type: WarningType) = WarningBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_TYPE, type) // WarningType is an enum (Serializable)
            }
        }
    }
}