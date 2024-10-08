package net.gini.android.health.sdk.review.openWith

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsBottomSheetOpenWithBinding
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.GhsBottomSheetDialogFragment
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthThemeAndLocale
import net.gini.android.health.sdk.util.getLocaleStringResource
import net.gini.android.health.sdk.util.setBackgroundTint

/**
 * Interface for forwarding the request to share a PDF document
 */
internal interface OpenWithForwardListener {
    fun onForwardSelected()
}
internal class OpenWithBottomSheet private constructor(paymentProviderApp: PaymentProviderApp?, private val listener: OpenWithForwardListener?, private val paymentComponent: PaymentComponent?) : GhsBottomSheetDialogFragment() {

    constructor(): this(null, null, null)

    private val viewModel by viewModels<OpenWithViewModel> { OpenWithViewModel.Factory(paymentProviderApp) }
    private var binding: GhsBottomSheetOpenWithBinding by autoCleared()

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniHealthThemeAndLocale(inflater, GiniHealth.getSDKLanguage(requireContext())?.languageLocale())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GhsBottomSheetOpenWithBinding.inflate(inflater, container, false)
        binding.ghsAppLayout.ghsAppName.ellipsize = TextUtils.TruncateAt.END
        binding.ghsAppLayout.ghsAppIcon.setImageDrawable(viewModel.paymentProviderApp?.icon)
        viewModel.paymentProviderApp?.let { paymentProviderApp ->
            with(binding.ghsForwardButton) {
                setOnClickListener {
                    listener?.onForwardSelected()
                    dismiss()
                }
                setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
                setTextColor(paymentProviderApp.colors.textColor)
            }
            binding.ghsAppLayout.ghsAppName.text = paymentProviderApp.name
            binding.ghsOpenWithTitle.text = String.format(getLocaleStringResource(R.string.ghs_open_with_title), paymentProviderApp.name)
            binding.ghsOpenWithDetails.text = String.format(getLocaleStringResource(R.string.ghs_open_with_details), paymentProviderApp.name)
            binding.ghsOpenWithInfo.text = createSpannableString(String.format(getLocaleStringResource(R.string.ghs_open_with_info), paymentProviderApp.name, paymentProviderApp.name), paymentProviderApp.paymentProvider.playStoreUrl)
            binding.ghsOpenWithInfo.movementMethod = LinkMovementMethod.getInstance()
        }
        binding.ghsMoreLayout.ghsAppName.text = getLocaleStringResource(R.string.ghs_open_with_more)
        with(binding.ghsMoreLayout.ghsAppIcon) {
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ghs_more_icon))
            elevation = 0f
            background = null
        }
        return binding.root
    }

    private fun createSpannableString(text: String, playStoreUrl: String?): SpannedString {
        val linkSpan = SpannableString(getLocaleStringResource(R.string.ghs_open_with_download_app))
        val playStoreLauncher = object: ClickableSpan() {
            override fun onClick(p0: View) {
                playStoreUrl?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(requireContext(), "No Play Store URL", Toast.LENGTH_LONG).show()
                }
            }

        }
        linkSpan.apply {
            setSpan(StyleSpan(Typeface.BOLD),0,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(playStoreLauncher, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(requireContext().getColor(R.color.ghs_open_with_details)),0,length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return buildSpannedString {
            append(text)
            append(" ")
            append(linkSpan)
            append(".")
        }
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, paymentComponent?.giniHealth)
    }

    companion object {
        /**
         * Create a new instance of the [OpenWithBottomSheet].
         *
         * @param paymentProviderApp the [PaymentProviderApp] which the user needs ti identify in the 'Share PDF' screen
         * @param listener the [OpenWithForwardListener] which will forward requests
         */
        fun newInstance(paymentProviderApp: PaymentProviderApp, paymentComponent: PaymentComponent?, listener: OpenWithForwardListener) = OpenWithBottomSheet(paymentProviderApp = paymentProviderApp, listener = listener, paymentComponent)
    }
}