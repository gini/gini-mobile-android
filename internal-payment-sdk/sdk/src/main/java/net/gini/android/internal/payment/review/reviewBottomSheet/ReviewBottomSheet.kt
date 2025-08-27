package net.gini.android.internal.payment.review.reviewBottomSheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.databinding.GpsBottomSheetReviewBinding
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.isLandscapeOrientation
import net.gini.android.internal.payment.utils.extensions.isViewModelInitialized
import net.gini.android.internal.payment.utils.extensions.onKeyboardAction
import net.gini.android.internal.payment.utils.extensions.setBackListener
import net.gini.android.internal.payment.utils.showKeyboard

private const val VIEW_SETTLE_DELAY_MS = 200L
private const val KEY_IME_WAS_VISIBLE = "ime_was_visible"
private const val KEY_FOCUSED_ID = "focused_view_id"
private const val KEYBOARD_VISIBILITY_RATIO = 0.25f
private var lastFocusedId: Int = View.NO_ID
private var focusTracker: ViewTreeObserver.OnGlobalFocusChangeListener? = null
class ReviewBottomSheet private constructor(
    private val viewModelFactory: ViewModelProvider.Factory?
) : GpsBottomSheetDialogFragment() {

    constructor() : this(null)

    private var imeVisibleNow: Boolean = false
    private var preRKeyboardTracker: ViewTreeObserver.OnGlobalLayoutListener? = null

    private val viewModel: ReviewBottomSheetViewModel by viewModels {
        viewModelFactory ?: object : ViewModelProvider.Factory {}
    }
    private var binding: GpsBottomSheetReviewBinding by autoCleared()
    private lateinit var bottomSheet: FrameLayout
    private val listener = object : ReviewViewListener {
        override fun onPaymentButtonTapped(paymentDetails: PaymentDetails) {
            viewModel.reviewViewListener?.onPaymentButtonTapped(paymentDetails)
        }

        override fun onSelectBankButtonTapped() {
            dismiss()
            viewModel.reviewViewListener?.onSelectBankButtonTapped()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            GiniInternalPaymentModule.getSDKLanguageInternal(requireContext())?.languageLocale()
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel.backListener?.let {
            (dialog as BottomSheetDialog).setBackListener(it)
        }

        dialog.setOnShowListener {
            bottomSheet = dialog
                .findViewById(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet).apply {
                isDraggable = true
                isCancelable = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Track the currently focused view’s id at all times
        focusTracker = ViewTreeObserver.OnGlobalFocusChangeListener { _, new ->
            val id = new?.id ?: View.NO_ID
            if (id != View.NO_ID) lastFocusedId = id
        }
        view.viewTreeObserver.addOnGlobalFocusChangeListener(focusTracker)

        if (preQ()) {
            startPreRKeyboardTracker(view)
            restoreFocusAndImeIfNeeded(view, savedInstanceState)
        }
    }

    private fun preQ() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    private fun restoreFocusAndImeIfNeeded(root: View, savedInstanceState: Bundle?) {
        val focusedId = savedInstanceState?.getInt(KEY_FOCUSED_ID) ?: View.NO_ID
        val imeWasVisible = savedInstanceState?.getBoolean(KEY_IME_WAS_VISIBLE) ?: false
        if (focusedId == View.NO_ID) return

        root.post {
            val v = root.findViewById<View>(focusedId)
            if (v != null && v.isShown && v.isEnabled && v.isFocusable) {
                v.requestFocus()
                // if you specifically want to re-open the IME:
                if (imeWasVisible && v is EditText) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(VIEW_SETTLE_DELAY_MS)
                        v.showKeyboard() // your helper
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModelFactory == null && !isViewModelInitialized(ReviewBottomSheetViewModel::class)) {
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GpsBottomSheetReviewBinding.inflate(inflater, container, false)
        with(binding.gpsReviewLayout) {
            reviewComponent = viewModel.reviewComponent
            listener = this@ReviewBottomSheet.listener
        }
        if (resources.isLandscapeOrientation()) {
            binding.dragHandle.setOnTouchListener { _, _ ->
                dismiss()
                false
            }
        }
        binding.dragHandle.onKeyboardAction {
            dismiss()
        }
        return binding.root
    }

    private fun startPreRKeyboardTracker(root: View) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(r)
            val visible = r.height()
            val heightDiff = root.rootView.height - visible
            imeVisibleNow = heightDiff > root.rootView.height * KEYBOARD_VISIBILITY_RATIO // ~keyboard threshold
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        preRKeyboardTracker = listener
    }

    override fun onDestroyView() {
        preRKeyboardTracker?.let { l ->
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(l)
        }
        preRKeyboardTracker = null
        super.onDestroyView()
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (preQ()) {
            outState.putInt(KEY_FOCUSED_ID, lastFocusedId)
            outState.putBoolean(KEY_IME_WAS_VISIBLE, imeVisibleNow)
        }
        super.onSaveInstanceState(outState)

    }
    companion object {
        fun newInstance(
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewViewListener,
            giniInternalPaymentModule: GiniInternalPaymentModule,
            backListener: BackListener
        ): ReviewBottomSheet {
            val factory = ReviewBottomSheetViewModel.Factory(
                paymentComponent = giniInternalPaymentModule.paymentComponent,
                reviewConfiguration = configuration,
                giniPaymentModule = giniInternalPaymentModule,
                backListener = backListener,
                reviewViewListener = listener
            )
            return ReviewBottomSheet(factory)
        }
    }
}