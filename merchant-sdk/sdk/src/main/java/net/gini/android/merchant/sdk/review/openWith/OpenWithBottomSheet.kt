package net.gini.android.merchant.sdk.review.openWith

import android.app.Dialog
import android.content.DialogInterface
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
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsBottomSheetOpenWithBinding
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.GmsBottomSheetDialogFragment
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.setBackListener
import net.gini.android.merchant.sdk.util.setBackgroundTint

/**
 * Interface for forwarding the request to share a PDF document
 */
internal interface OpenWithForwardListener {
    fun onForwardSelected()
}
internal class OpenWithBottomSheet private constructor(paymentProviderApp: PaymentProviderApp?, private val listener: OpenWithForwardListener?, private val backListener: BackListener?) : GmsBottomSheetDialogFragment() {

    constructor(): this(null, null, null)

    private val viewModel by viewModels<OpenWithViewModel> { OpenWithViewModel.Factory(paymentProviderApp, backListener) }
    private var binding: GmsBottomSheetOpenWithBinding by autoCleared()

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
        binding = GmsBottomSheetOpenWithBinding.inflate(inflater, container, false)
        binding.gmsAppLayout.gmsAppName.ellipsize = TextUtils.TruncateAt.END
        binding.gmsAppLayout.gmsAppIcon.setImageDrawable(viewModel.paymentProviderApp?.icon)
        viewModel.paymentProviderApp?.let { paymentProviderApp ->
            with(binding.gmsForwardButton) {
                setOnClickListener {
                    listener?.onForwardSelected()
                    dismiss()
                }
                setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
                setTextColor(paymentProviderApp.colors.textColor)
            }
            binding.gmsAppLayout.gmsAppName.text = paymentProviderApp.name
            binding.gmsOpenWithTitle.text = String.format(getString(R.string.gms_open_with_title), paymentProviderApp.name)
            binding.gmsOpenWithDetails.text = String.format(getString(R.string.gms_open_with_details), paymentProviderApp.name)
            binding.gmsOpenWithInfo.text = createSpannableString(String.format(getString(R.string.gms_open_with_info), paymentProviderApp.name, paymentProviderApp.name), paymentProviderApp.paymentProvider.playStoreUrl)
            binding.gmsOpenWithInfo.movementMethod = LinkMovementMethod.getInstance()
        }
        return binding.root
    }

    private fun createSpannableString(text: String, playStoreUrl: String?): SpannedString {
        val linkSpan = SpannableString(getString(R.string.gms_open_with_download_app))
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
            setSpan(ForegroundColorSpan(requireContext().getColor(R.color.gms_open_with_details)),0,length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return buildSpannedString {
            append(text)
            append(" ")
            append(linkSpan)
            append(".")
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        viewModel.backListener?.backCalled()
        super.onCancel(dialog)
    }

    companion object {
        /**
         * Create a new instance of the [OpenWithBottomSheet].
         *
         * @param paymentProviderApp the [PaymentProviderApp] which the user needs ti identify in the 'Share PDF' screen
         * @param listener the [OpenWithForwardListener] which will forward requests
         * @param backListener the [BackListener] which will forward back events
         */
        fun newInstance(paymentProviderApp: PaymentProviderApp, listener: OpenWithForwardListener, backListener: BackListener? = null) = OpenWithBottomSheet(paymentProviderApp = paymentProviderApp, listener = listener, backListener = backListener)
    }
}