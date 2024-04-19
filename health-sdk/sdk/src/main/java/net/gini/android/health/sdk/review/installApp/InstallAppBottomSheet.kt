package net.gini.android.health.sdk.review.installApp

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetInstallAppBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme
import net.gini.android.health.sdk.util.setBackgroundTint
import net.gini.android.health.sdk.util.wrappedWithGiniHealthTheme
import org.slf4j.LoggerFactory


interface InstallAppForwardListener {
    fun onForwardToBankSelected()
}

internal class InstallAppBottomSheet private constructor(
    private val paymentComponent: PaymentComponent?,
    private val paymentProviderApp: PaymentProviderApp?,
    private val listener: InstallAppForwardListener?
) :
    BottomSheetDialogFragment() {
    constructor() : this(null, null, null)

    private var binding: GhsBottomSheetInstallAppBinding by autoCleared()
    private val viewModel: InstallAppViewModel by viewModels {
        InstallAppViewModel.Factory(
            paymentProviderApp
        )
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniHealthTheme(inflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val wrappedContext = requireContext().wrappedWithGiniHealthTheme()
        val dialog = BottomSheetDialog(wrappedContext, theme)

        val colorDrawable =
            ColorDrawable(ContextCompat.getColor(wrappedContext, R.color.ghs_bottom_sheet_scrim))
        colorDrawable.alpha = 102 // 40% alpha
        dialog.window?.setBackgroundDrawable(colorDrawable)

        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GhsBottomSheetInstallAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        paymentComponent?.recheckWhichPaymentProviderAppsAreInstalled()
        viewModel.paymentProviderApp?.let { paymentProviderApp ->
            binding.ghsPaymentProviderIcon.ghsPaymentProviderIcon.setImageDrawable(
                paymentProviderApp.icon
            )
            binding.ghsPaymentProviderIcon.ghsPaymentProviderIcon.contentDescription = "${paymentProviderApp.name} ${getString(R.string.ghs_payment_provider_logo_content_description)}"
            binding.ghsInstallAppTitle.text = String.format(
                getString(net.gini.android.health.sdk.R.string.ghs_install_app_title),
                paymentProviderApp.paymentProvider.name
            )
            binding.ghsInstallAppDetails.text = String.format(
                getString(net.gini.android.health.sdk.R.string.ghs_install_app_detail),
                paymentProviderApp.paymentProvider.name
            )
            binding.ghsPlayStoreLogo.setOnClickListener {
                paymentProviderApp.paymentProvider.playStoreUrl?.let { openPlayStoreUrl(it) }
            }
            if (paymentComponent?.isPaymentProviderAppInstalled(paymentProviderApp.paymentProvider.id) == true) {
                updateUI()
            } else {
                resetUI()
            }
        }
    }

    private fun updateUI() {
        binding.ghsInstallAppDetails.text = String.format(
            getString(R.string.ghs_install_app_tap_to_continue),
            viewModel.paymentProviderApp?.paymentProvider?.name
        )
        binding.ghsPlayStoreLogo.visibility = View.GONE
        changeBottomConstraintOfDetailsLabel(
            R.id.ghs_forward_button,
            resources.getDimension(R.dimen.ghs_large_24).toInt()
        )
        binding.ghsForwardButton.apply {
            viewModel.paymentProviderApp?.let { paymentProviderApp ->
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
        changeBottomConstraintOfDetailsLabel(
            R.id.ghs_play_store_logo,
            resources.getDimension(R.dimen.ghs_medium).toInt()
        )
        binding.ghsPlayStoreLogo.visibility = View.VISIBLE
    }

    private fun changeBottomConstraintOfDetailsLabel(itemToConstrainTo: Int, margin: Int) {
        val constraintLayout: ConstraintLayout = binding.root
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.clear(R.id.ghs_install_app_details, ConstraintSet.BOTTOM)
        constraintSet.connect(
            R.id.ghs_install_app_details,
            ConstraintSet.BOTTOM,
            itemToConstrainTo,
            ConstraintSet.TOP,
            margin
        )
        constraintSet.applyTo(constraintLayout)
    }

    private fun openPlayStoreUrl(playStoreUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InstallAppBottomSheet::class.java)

        /**
         * Create a new instance of the [InstallAppBottomSheet].
         *
         * @param paymentProviderApp the [PaymentProviderApp] which needs to be installed in order to be used to make the payment
         */
        fun newInstance(
            paymentComponent: PaymentComponent,
            paymentProviderApp: PaymentProviderApp,
            listener: InstallAppForwardListener
        ): InstallAppBottomSheet {
            return InstallAppBottomSheet(paymentComponent, paymentProviderApp, listener)
        }
    }

}