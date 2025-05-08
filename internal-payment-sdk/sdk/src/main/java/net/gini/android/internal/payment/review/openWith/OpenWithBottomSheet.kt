package net.gini.android.internal.payment.review.openWith

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
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
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
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
            binding.gpsAmountValue.text = it.amount
            binding.gpsRecipientValue.text = it.recipient
            binding.gpsReferenceValue.text = it.purpose
        }
    }

    private fun configureForwardButton() {
        viewModel.paymentProviderApp?.let { app ->
            with(binding.gpsForwardButton) {
                setOnClickListener {
                    viewModel.openWithForwardListener?.onForwardSelected()
                    dismiss()
                }
                setBackgroundTint(app.colors.backgroundColor, 255)
                setTextColor(app.colors.textColor)
                text = getString(R.string.gps_open_with_button_text, app.name)
                contentDescription = getString(R.string.gps_share_with_button_content_description, app.name)

                app.icon?.let { icon ->
                    val drawable = RoundedBitmapDrawableFactory.create(resources, icon.bitmap).apply {
                        cornerRadius = resources.getDimension(R.dimen.gps_small_2)
                    }
                    setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                }
            }

            binding.gpsOpenWithTitle.text = getString(R.string.gps_open_with_title, app.name)
            binding.gpsOpenWithDetails.text = getString(R.string.gps_open_with_details, app.name)
        }
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
