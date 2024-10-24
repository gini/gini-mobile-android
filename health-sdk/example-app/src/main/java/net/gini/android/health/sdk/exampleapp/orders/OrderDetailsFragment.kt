package net.gini.android.health.sdk.exampleapp.orders

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.databinding.FragmentOrderDetailsBinding
import net.gini.android.health.sdk.exampleapp.invoices.ui.InvoicesViewModel
import net.gini.android.health.sdk.exampleapp.orders.model.Order
import net.gini.android.health.sdk.exampleapp.orders.model.getPaymentDetails
import net.gini.android.health.sdk.util.hideKeyboard
import net.gini.android.internal.payment.utils.extensions.setIntervalClickListener
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat

class OrderDetailsFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailsBinding
    private val invoicesViewModel: InvoicesViewModel by activityViewModels()
    private val orderDetailsViewModel: OrderDetailsViewModel by viewModels()
    private val amountWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            s?.let { input ->
                // Take only the first 7 digits (without leading zeros)
                val onlyDigits = input.toString().trim()
                    .filter { it != '.' && it != ',' }
                    .take(7)
                    .trimStart('0')

                val newString = try {
                    // Parse to a decimal with two decimal places
                    val decimal = BigDecimal(onlyDigits).divide(BigDecimal(100))
                    // Format to a currency string
                    currencyFormatterWithoutSymbol().format(decimal).trim()
                } catch (e: NumberFormatException) {
                    ""
                }

                if (newString != input.toString()) {
                    input.replace(0, input.length, newString)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    orderDetailsViewModel.orderFlow.collectLatest { order ->
                        showOrder(order)
                    }
                }
                launch {
                    invoicesViewModel.openBankState.collect { openBankState ->
                        if (openBankState is GiniHealth.PaymentState.Success) {
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
            }
        }
        setupInputListeners()
    }

    private fun showOrder(order: Order) {
        with(binding) {
            recipient.setTextIfDifferent(order.recipient)
            iban.setTextIfDifferent(order.iban)
            amount.setTextIfDifferent(order.amount)
            purpose.setTextIfDifferent(order.purpose)
            payNowBtn.setIntervalClickListener {
                this.root.hideKeyboard()
                invoicesViewModel.startPaymentFlowWithoutDocument(orderDetailsViewModel.getOrder().getPaymentDetails())
            }
        }
    }

    private fun setupInputListeners() {
        with(binding) {
            recipient.addTextChangedListener { text ->
                orderDetailsViewModel.updateRecipient(text.toString())
            }
            iban.addTextChangedListener { text ->
                orderDetailsViewModel.updateIBAN(text.toString())
            }
            amount.addTextChangedListener { text ->
                orderDetailsViewModel.updateAmount(text.toString())
            }
            amount.addTextChangedListener(amountWatcher)
            purpose.addTextChangedListener { text ->
                orderDetailsViewModel.updatePurpose(text.toString())
            }
        }
    }

    fun currencyFormatterWithoutSymbol(): NumberFormat =
        NumberFormat.getCurrencyInstance().apply {
            (this as? DecimalFormat)?.apply {
                decimalFormatSymbols = decimalFormatSymbols.apply {
                    currencySymbol = ""
                }
            }
        }

    companion object {
        fun newInstance() = OrderDetailsFragment()
    }
}

private fun TextInputEditText.setTextIfDifferent(text: String) {
    if (this.text.toString() != text) {
        this.setText(text)
    }
}