package net.gini.android.internal.payment.review.openWith

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import net.gini.android.health.api.response.IngredientBrandType
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.databinding.GpsBottomSheetOpenWithBinding
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.addEuroSymbol
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
import net.gini.android.internal.payment.utils.extensions.isLandscapeOrientation
import net.gini.android.internal.payment.utils.extensions.onKeyboardAction
import net.gini.android.internal.payment.utils.extensions.setBackListener
import net.gini.android.internal.payment.utils.setBackgroundTint

/**
 * Interface for forwarding the request to share a PDF document
 */
interface OpenWithForwardListener {
    fun onForwardSelected()
}

class OpenWithBottomSheet private constructor(
    paymentProviderApp: PaymentProviderApp?,
    private val listener: OpenWithForwardListener?,
    private val backListener: BackListener?,
    paymentComponent: PaymentComponent?,
    paymentDetails: PaymentDetails?,
    paymentRequestId: String?
) : GpsBottomSheetDialogFragment() {

    constructor() : this(null, null, null, null, null, null)

    private val viewModel by viewModels<OpenWithViewModel> {
        OpenWithViewModel.Factory(
            paymentComponent,
            paymentProviderApp,
            listener,
            backListener,
            paymentDetails,
            paymentRequestId
        )
    }
    private var binding: GpsBottomSheetOpenWithBinding by autoCleared()
    private val internalPaymentModule
        get() = viewModel.paymentComponent?.paymentModule

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            GiniInternalPaymentModule.getSDKLanguageInternal(requireContext())?.languageLocale()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.isRestoredAfterProcessDeath) {
            dismissAllowingStateLoss()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel.backListener?.let {
            (dialog as BottomSheetDialog).setBackListener(it)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GpsBottomSheetOpenWithBinding.inflate(inflater, container, false)

        setupLifecycleObservers()
        setupKeyboardDismiss()
        populatePaymentDetails()
        configureForwardButton()
        configureBrandVisibility()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.gps_open_with_title))
        evaluateMarginForDynamicAnchor()
    }

    private fun evaluateMarginForDynamicAnchor() {
        if (!resources.isLandscapeOrientation()) return
        binding.scrollView?.post {
            binding.gpsDynamicAnchor?.let { safeDynamicAnchor ->
                val params = safeDynamicAnchor.layoutParams as ConstraintLayout.LayoutParams
                params.topToTop = ConstraintLayout.LayoutParams.UNSET
                params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET

                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.topMargin = maxOf(binding.gpsRecipientValue.bottom, binding.gpsIbanValue.bottom)

                safeDynamicAnchor.layoutParams = params
                binding.gpsOpenWithDetailsContainer?.requestLayout()
            }
        }
    }

    private fun setupLifecycleObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadPaymentRequestQrCode()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.qrCodeFlow.collect { qrCode ->
                    binding.gpsQrImageView.setImageBitmap(qrCode)
                }
            }
        }
    }

    private fun setupKeyboardDismiss() {
        binding.dragHandle.onKeyboardAction {
            dismiss()
        }
    }

    private fun populatePaymentDetails() {
        viewModel.paymentDetails?.let {
            binding.gpsIbanValue.text = it.iban
            binding.gpsAmountValue.text = it.amount.addEuroSymbol()
            binding.gpsRecipientValue.text = it.recipient
            binding.gpsReferenceValue.text = it.purpose
        }
    }


    private fun configureForwardButton() {
        viewModel.paymentProviderApp?.let { app ->
            setupForwardButtonClickListener()
            styleForwardButton(app)
            updateOpenWithTexts(app)
            setAppIcon(app)
            setForwardButtonTextAndContentDescription(app)
        }
    }

    private fun setupForwardButtonClickListener() {
        binding.gpsForwardButton.setOnClickListener {
            viewModel.openWithForwardListener?.onForwardSelected()
            dismiss()
        }
    }

    private fun styleForwardButton(app: PaymentProviderApp) {
        binding.gpsForwardButton.setBackgroundTint(app.colors.backgroundColor, 255)
        binding.gpsForwardButton.setTextColor(app.colors.textColor)
    }

    private fun updateOpenWithTexts(app: PaymentProviderApp) {
        binding.gpsOpenWithTitle.text = String.format(
            getLocaleStringResource(R.string.gps_open_with_title),
            app.name
        )
        binding.gpsOpenWithDetails.text = String.format(
            getLocaleStringResource(R.string.gps_open_with_details),
            app.name
        )
    }

    private fun setAppIcon(app: PaymentProviderApp) {
        app.icon?.let { icon ->
            val roundedDrawable = RoundedBitmapDrawableFactory
                .create(requireContext().resources, icon.bitmap)
                .apply {
                    cornerRadius = resources.getDimension(R.dimen.gps_small_2)
                }

            binding.gpsForwardButton.setCompoundDrawablesWithIntrinsicBounds(
                null, null, roundedDrawable, null
            )
        }
    }

    private fun setForwardButtonTextAndContentDescription(app: PaymentProviderApp) {
        binding.gpsForwardButton.text = String.format(
            getLocaleStringResource(R.string.gps_open_with_button_text),
            app.name
        )

        binding.gpsForwardButton.contentDescription = String.format(
            getLocaleStringResource(R.string.gps_share_with_button_content_description),
            app.name
        )
    }

    private fun configureBrandVisibility() {
        binding.gpsPoweredByGiniLayout.root.visibility =
            if (internalPaymentModule?.getIngredientBrandVisibility() == IngredientBrandType.FULL_VISIBLE)
                View.VISIBLE else View.INVISIBLE
    }


    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.paymentComponent?.paymentModule)
    }

    companion object {
        /**
         * Create a new instance of the [OpenWithBottomSheet].
         *
         * @param paymentProviderApp the [PaymentProviderApp] which the user needs ti identify in the 'Share PDF' screen
         * @param listener the [OpenWithForwardListener] which will forward requests
         * @param backListener the [BackListener] which will forward back events
         */
        fun newInstance(
            paymentProviderApp: PaymentProviderApp,
            listener: OpenWithForwardListener,
            paymentComponent: PaymentComponent?,
            backListener: BackListener? = null,
            paymentDetails: PaymentDetails?,
            paymentRequestId: String?
        ) = OpenWithBottomSheet(
            paymentProviderApp = paymentProviderApp,
            listener = listener,
            backListener = backListener,
            paymentComponent = paymentComponent,
            paymentDetails = paymentDetails,
            paymentRequestId = paymentRequestId
        )
    }
}
