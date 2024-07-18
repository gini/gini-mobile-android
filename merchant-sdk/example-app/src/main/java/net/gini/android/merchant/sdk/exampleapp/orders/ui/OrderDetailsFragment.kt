package net.gini.android.merchant.sdk.exampleapp.orders.ui

import android.os.Bundle
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
import net.gini.android.merchant.sdk.exampleapp.databinding.FragmentOrderDetailsBinding
import net.gini.android.merchant.sdk.exampleapp.orders.data.model.Order
import net.gini.android.merchant.sdk.util.setIntervalClickListener

class OrderDetailsFragment : Fragment() {

    private lateinit var binding: FragmentOrderDetailsBinding
    private val ordersViewModel: OrdersViewModel by activityViewModels<OrdersViewModel>()
    private val orderDetailsViewModel: OrderDetailsViewModel by viewModels()

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
                    ordersViewModel.selectedOrderItem.collectLatest { orderItem ->
                        if (orderItem == null) {
                            return@collectLatest
                        }
                        showOrder(orderItem.order)
                        orderDetailsViewModel.setOrder(orderItem.order)
                    }
                }
                launch {
                    orderDetailsViewModel.orderFlow.collectLatest { order ->
                        showOrder(order)
                    }
                }
                launch {
                    ordersViewModel.finishPaymentFlow.collect {
                        if (it == true) {
                            requireActivity().supportFragmentManager.popBackStack()
                            ordersViewModel.resetFinishPaymentFlow()
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
                ordersViewModel.startPaymentFlow()
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
            purpose.addTextChangedListener { text ->
                orderDetailsViewModel.updatePurpose(text.toString())
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