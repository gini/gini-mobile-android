package net.gini.android.internal.payment.review.installApp

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import net.gini.android.health.api.response.IngredientBrandType
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsBottomSheetInstallAppBinding
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.GpsBottomSheetDialogFragment
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
import net.gini.android.internal.payment.utils.extensions.setBackListener
import net.gini.android.internal.payment.utils.setBackgroundTint
import org.slf4j.LoggerFactory

/**
 * Interface for forwarding the request to redirect to bank app.
 */
interface InstallAppForwardListener {
    fun onForwardToBankSelected()
}

class InstallAppBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?,
    private val listener: InstallAppForwardListener?,
    backListener: BackListener?,
    private val minHeight: Int?
) :
    GpsBottomSheetDialogFragment() {
    constructor() : this(null, null, null, null)

    private var binding: GpsBottomSheetInstallAppBinding by autoCleared()
    private val viewModel: InstallAppViewModel by viewModels {
        InstallAppViewModel.Factory(
            paymentComponent,
            backListener
        )
    }

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
        binding = GpsBottomSheetInstallAppBinding.inflate(inflater, container, false)
        binding.root.minHeight = minHeight ?: resources.getDimension(R.dimen.gps_install_app_min_height).toInt()
        binding.gpsPoweredByGiniLayout.root.visibility = if (paymentComponent?.paymentModule?.getIngredientBrandVisibility() == IngredientBrandType.FULL_VISIBLE) View.VISIBLE else View.INVISIBLE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.paymentProviderApp.collect { paymentProviderApp ->
                    if (paymentProviderApp != null) {
                        binding.gpsPaymentProviderIcon.gpsPaymentProviderIcon.setImageDrawable(
                            paymentProviderApp.icon
                        )
                        binding.gpsPaymentProviderIcon.gpsPaymentProviderIcon.contentDescription =
                            "${paymentProviderApp.name} ${getString(R.string.gps_payment_provider_logo_content_description)}"
                        binding.gpsInstallAppTitle.text = String.format(
                            getLocaleStringResource(R.string.gps_install_app_title),
                            paymentProviderApp.paymentProvider.name
                        )
                        binding.gpsInstallAppDetails.text = String.format(
                            getLocaleStringResource(R.string.gps_install_app_detail),
                            paymentProviderApp.paymentProvider.name
                        )
                        binding.gpsPlayStoreLogo.setOnClickListener {
                            paymentProviderApp.paymentProvider.playStoreUrl?.let { openPlayStoreUrl(it) }
                        }
                        if (paymentProviderApp.isInstalled()) {
                            updateUI(paymentProviderApp)
                        } else {
                            resetUI()
                        }
                    } else {
                        LOG.error("No selected payment provider app")
                    }
                }
            }
        }
    }

    private fun updateUI(paymentProviderApp: PaymentProviderApp) {
        binding.gpsInstallAppDetails.text = String.format(
            getLocaleStringResource(R.string.gps_install_app_tap_to_continue),
            paymentProviderApp.paymentProvider.name
        )
        binding.gpsPlayStoreLogo.visibility = View.GONE
        binding.gpsForwardButton.apply {
            paymentProviderApp.let { paymentProviderApp ->
                setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
                setTextColor(paymentProviderApp.colors.textColor)
            }
            visibility = View.VISIBLE
        }

        binding.gpsForwardButton.setOnClickListener {
            listener?.onForwardToBankSelected()
            dismiss()
        }
    }

    private fun resetUI() {
        binding.gpsForwardButton.visibility = View.GONE
        binding.gpsPlayStoreLogo.visibility = View.VISIBLE
    }

    private fun openPlayStoreUrl(playStoreUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.paymentComponent?.paymentModule)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InstallAppBottomSheet::class.java)

        /**
         * Create a new instance of the [InstallAppBottomSheet].
         *
         * @param paymentComponent the [PaymentComponent] which is needed to check the installation state of the payment provider app
         * @param listener the [InstallAppForwardListener] which will forward redirect requests
         */
        fun newInstance(
            paymentComponent: PaymentComponent,
            listener: InstallAppForwardListener,
            backListener: BackListener?,
            minHeight: Int?
        ): InstallAppBottomSheet {
            return InstallAppBottomSheet(paymentComponent, listener, backListener, minHeight)
        }
    }
}
