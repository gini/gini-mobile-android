package net.gini.android.internal.payment.review.openWith

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import net.gini.android.internal.payment.utils.extensions.setBackListener
import net.gini.android.internal.payment.utils.setBackgroundTint

/**
 * Interface for forwarding the request to share a PDF document
 */
interface OpenWithForwardListener {
    fun onForwardSelected()
}
class OpenWithBottomSheet private constructor(paymentProviderApp: PaymentProviderApp?, private val listener: OpenWithForwardListener?, private val backListener: BackListener?, paymentComponent: PaymentComponent?, paymentDetails: PaymentDetails?) : GpsBottomSheetDialogFragment() {

    constructor(): this(null, null, null, null, null)

    private val viewModel by viewModels<OpenWithViewModel> {
        OpenWithViewModel.Factory(
            paymentComponent,
            paymentProviderApp,
            listener,
            backListener,
            paymentDetails
        )
    }
    private var binding: GpsBottomSheetOpenWithBinding by autoCleared()

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            GiniInternalPaymentModule.getSDKLanguage(requireContext())?.languageLocale()
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
        viewModel.paymentDetails?.let {
            binding.gpsIbanValue.text = it.iban
            binding.gpsAmountValue.text = it.amount
            binding.gpsRecipientValue.text = it.recipient
            binding.gpsReferenceValue.text = it.purpose
        }
        viewModel.paymentProviderApp?.let { paymentProviderApp ->
            with(binding.gpsForwardButton) {
                setOnClickListener {
                    viewModel.openWithForwardListener?.onForwardSelected()
                    dismiss()
                }
                setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
                setTextColor(paymentProviderApp.colors.textColor)
            }
            binding.gpsForwardButton.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
            binding.gpsForwardButton.setTextColor(paymentProviderApp.colors.textColor)
            binding.gpsOpenWithTitle.text =
                String.format(getLocaleStringResource(R.string.gps_open_with_title), paymentProviderApp.name)
            binding.gpsOpenWithDetails.text =
                String.format(getLocaleStringResource(R.string.gps_open_with_details), paymentProviderApp.name)

            paymentProviderApp.icon?.let { appIcon ->
                val roundedDrawable =
                    RoundedBitmapDrawableFactory.create(requireContext().resources, appIcon.bitmap).apply {
                        cornerRadius = resources.getDimension(R.dimen.gps_small_2)
                    }

                binding.gpsForwardButton.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    roundedDrawable,
                    null
                )
            }
            binding.gpsForwardButton.text = String.format(getLocaleStringResource(R.string.gps_open_with_button_text), paymentProviderApp.name)
        }
        return binding.root
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
        fun newInstance(paymentProviderApp: PaymentProviderApp, listener: OpenWithForwardListener, paymentComponent: PaymentComponent?, backListener: BackListener? = null, paymentDetails: PaymentDetails?) = OpenWithBottomSheet(paymentProviderApp = paymentProviderApp, listener = listener, backListener = backListener, paymentComponent = paymentComponent, paymentDetails = paymentDetails)
    }
}