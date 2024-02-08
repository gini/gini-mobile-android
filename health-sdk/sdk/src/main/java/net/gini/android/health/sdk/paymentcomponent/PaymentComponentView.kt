package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsViewPaymentComponentBinding
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme

class PaymentComponentView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    var isPayable: Boolean = false
        set(isPayable) {
            field = isPayable
            if (isPayable) {
                show()
            } else {
                hide()
            }
        }

    private val binding = GhsViewPaymentComponentBinding.inflate(getLayoutInflaterWithGiniHealthTheme(), this)

    init {
        setupMoreInformationLabelAndIcon()
    }

    private fun setupMoreInformationLabelAndIcon() {
        binding.ghsMoreInformationLabel.movementMethod = LinkMovementMethod.getInstance()
        addMoreInformationUnderlinedClickableText {
            onMoreInformationClicked()
        }
        binding.ghsInfoCircleIcon.setOnClickListener {
            onMoreInformationClicked()
        }
    }

    fun prepareForReuse() {
        isPayable = false
    }

    private fun show() {
        binding.ghsPayInvoiceButton.visibility = VISIBLE
        binding.ghsMoreInformationLabel.visibility = VISIBLE
        binding.ghsSelectBankLabel.visibility = VISIBLE
        binding.ghsSelectBankPicker.visibility = VISIBLE
        binding.ghsInfoCircleIcon.visibility = VISIBLE
        binding.ghsPoweredByGiniLabel.visibility = VISIBLE
        binding.ghsGiniLogo.visibility = VISIBLE
    }

    private fun hide() {
        binding.ghsPayInvoiceButton.visibility = GONE
        binding.ghsMoreInformationLabel.visibility = GONE
        binding.ghsSelectBankLabel.visibility = GONE
        binding.ghsSelectBankPicker.visibility = GONE
        binding.ghsInfoCircleIcon.visibility = GONE
        binding.ghsPoweredByGiniLabel.visibility = GONE
        binding.ghsGiniLogo.visibility = GONE
    }

    private fun addMoreInformationUnderlinedClickableText(clickListener: () -> Unit) {
        binding.ghsMoreInformationLabel.text = buildSpannedString {
            append(ContextCompat.getString(context, R.string.ghs_more_information_label))
            append(" ")
            append(buildSpannedString {
                color(ContextCompat.getColor(context, R.color.ghs_payment_component_caption)) {
                    bold {
                        inSpans(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                clickListener()
                            }
                        }) {
                            val underlinedPart =
                                ContextCompat.getString(context, R.string.ghs_more_information_underlined_part)
                            append(underlinedPart.replace(" ".toRegex(), "\u00A0"))
                        }
                    }
                }

            })
        }
    }

    private fun onMoreInformationClicked() {
        Log.d("PaymentComponentView", "More information clicked")
    }
}