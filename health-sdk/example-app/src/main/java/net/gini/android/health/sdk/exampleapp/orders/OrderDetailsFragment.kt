package net.gini.android.health.sdk.exampleapp.orders

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.FragmentOrderDetailsBinding
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order
import net.gini.android.health.sdk.exampleapp.orders.data.model.getPaymentDetails
import net.gini.android.health.sdk.exampleapp.util.isInTheFuture
import net.gini.android.health.sdk.exampleapp.util.prettifyDate
import net.gini.android.health.sdk.util.hideKeyboard
import net.gini.android.internal.payment.utils.DisplayedScreen
import net.gini.android.internal.payment.utils.extensions.setIntervalClickListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import net.gini.android.internal.payment.R as internalR

class OrderDetailsFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailsBinding
    private val ordersViewModel: OrdersViewModel by activityViewModels()
    private val orderDetailsViewModel: OrderDetailsViewModel by viewModel()
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
        requireActivity().title = resources.getString(R.string.title_create_order)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    ordersViewModel.selectedOrderItem.collectLatest { orderItem ->
                        if (orderItem == null) {
                            return@collectLatest
                        }
                        orderDetailsViewModel.setOrder(orderItem.order)
                    }
                }
                launch {
                    orderDetailsViewModel.orderFlow.collectLatest { order ->
                        showOrder(order)
                    }
                }
                launch {
                    ordersViewModel.openBankState.collect { openBankState ->
                        if (openBankState is GiniHealth.PaymentState.Success) {
                            requireActivity().title = resources.getString(R.string.title_activity_invoices)
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
                launch {
                    ordersViewModel.displayedScreen.collect { screen ->
                        setTitle(screen)
                    }
                }
                launch {
                    orderDetailsViewModel.errorFlow.collect { error ->
                        when (error) {
                            is OrderDetailsViewModel.Error.ErrorMessage -> showError(error.error)
                            OrderDetailsViewModel.Error.GenericError -> showError(getString(internalR.string.gps_generic_error_message))
                            OrderDetailsViewModel.Error.InvalidIban -> showError(getString(internalR.string.gps_error_input_invalid_iban))
                            OrderDetailsViewModel.Error.PaymentDetailsIncomplete -> showError(getString(R.string.payment_details_incomplete))
                            null -> Unit
                        }
                    }
                }
            }
        }
        setupInputListeners()
    }

    private fun setTitle(screen: DisplayedScreen) {
        requireActivity().title = if (screen is DisplayedScreen.MoreInformationFragment) {
            resources.getString(net.gini.android.internal.payment.R.string.gps_more_information_fragment_title)
        } else {
            resources.getString(R.string.title_create_order)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showOrder(order: Order) {
        with(binding) {
            recipient.setTextIfDifferent(order.recipient)
            iban.setTextIfDifferent(order.iban)
            amount.setTextIfDifferent(order.amount)
            purpose.setTextIfDifferent(order.purpose)
            payNowBtn.isEnabled = order.expiryDate.isNullOrEmpty() || order.expiryDate?.isInTheFuture() == true
            payNowBtn.setIntervalClickListener {
                this.root.hideKeyboard()
                ordersViewModel.saveOrderToLocal(order)
                ordersViewModel.startPaymentFlowWithoutDocument(orderDetailsViewModel.getOrder().getPaymentDetails())
            }
            createPaymentRequestBtn.setIntervalClickListener {
                if (orderDetailsViewModel.arePaymentDetailsValid()) {
                    orderDetailsViewModel.createPaymentRequest()
                }
            }
            order.expiryDate?.let {
                expirationDate.text = "${getString(R.string.expiration_date)} ${it.prettifyDate()}"
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

    private fun showError(message: String) {
        AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.create_payment_request_error))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun currencyFormatterWithoutSymbol(): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.GERMAN).apply {
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