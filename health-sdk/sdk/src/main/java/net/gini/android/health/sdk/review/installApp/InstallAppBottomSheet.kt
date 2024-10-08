package net.gini.android.health.sdk.review.installApp

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
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetInstallAppBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.GhsBottomSheetDialogFragment
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthThemeAndLocale
import net.gini.android.health.sdk.util.getLocaleStringResource
import net.gini.android.health.sdk.util.setBackgroundTint
import org.slf4j.LoggerFactory

/**
 * Interface for forwarding the request to redirect to bank app.
 */
interface InstallAppForwardListener {
    fun onForwardToBankSelected()
}

internal class InstallAppBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?,
    private val listener: InstallAppForwardListener?,
    private val minHeight: Int?
) :
    GhsBottomSheetDialogFragment() {
    constructor() : this(null, null, null)

    private var binding: GhsBottomSheetInstallAppBinding by autoCleared()
    private val viewModel: InstallAppViewModel by viewModels {
        InstallAppViewModel.Factory(
            paymentComponent
        )
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniHealthThemeAndLocale(inflater, GiniHealth.getSDKLanguage(requireContext())?.languageLocale())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GhsBottomSheetInstallAppBinding.inflate(inflater, container, false)
        minHeight?.let {
            binding.root.minHeight = it
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.paymentProviderApp.collect { paymentProviderApp ->
                    if (paymentProviderApp != null) {
                        binding.ghsPaymentProviderIcon.ghsPaymentProviderIcon.setImageDrawable(
                            paymentProviderApp.icon
                        )
                        binding.ghsPaymentProviderIcon.ghsPaymentProviderIcon.contentDescription =
                            "${paymentProviderApp.name} ${getLocaleStringResource(R.string.ghs_payment_provider_logo_content_description)}"
                        binding.ghsInstallAppTitle.text = String.format(
                            getLocaleStringResource(net.gini.android.health.sdk.R.string.ghs_install_app_title),
                            paymentProviderApp.paymentProvider.name
                        )
                        binding.ghsInstallAppDetails.text = String.format(
                            getLocaleStringResource(net.gini.android.health.sdk.R.string.ghs_install_app_detail),
                            paymentProviderApp.paymentProvider.name
                        )
                        binding.ghsPlayStoreLogo.setOnClickListener {
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
        binding.ghsInstallAppDetails.text = String.format(
            getLocaleStringResource(R.string.ghs_install_app_tap_to_continue),
            paymentProviderApp.paymentProvider.name
        )
        binding.ghsPlayStoreLogo.visibility = View.GONE
        binding.ghsForwardButton.apply {
            paymentProviderApp.let { paymentProviderApp ->
                setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
                setTextColor(paymentProviderApp.colors.textColor)
            }
            visibility = View.VISIBLE
        }

        binding.ghsForwardButton.setOnClickListener {
            listener?.onForwardToBankSelected()
            dismiss()
        }
    }

    private fun resetUI() {
        binding.ghsForwardButton.visibility = View.GONE
        binding.ghsPlayStoreLogo.visibility = View.VISIBLE
    }

    private fun openPlayStoreUrl(playStoreUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.paymentComponent?.giniHealth)
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
            minHeight: Int
        ): InstallAppBottomSheet {
            return InstallAppBottomSheet(paymentComponent, listener, minHeight)
        }
    }

}