package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.databinding.FragmentInvoiceDetailsBinding
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.setIntervalClickListener

class InvoiceDetailsFragment: Fragment() {

    private lateinit var binding: FragmentInvoiceDetailsBinding
    private val viewModel: InvoicesViewModel by activityViewModels<InvoicesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentInvoiceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.selectedInvoiceItem.collectLatest { invoice ->
                        binding.invoiceNumber.text = invoice?.documentId
                        binding.amount.text = invoice?.amount
                        binding.dueDate.text = invoice?.dueDate
                        binding.payNowBtn.setIntervalClickListener {
                            viewModel.startIntegratedPaymentFlow(invoice?.documentId ?: "")
                        }
                    }
                }
                launch {
                    viewModel.giniMerchant.eventsFlow.collect { event ->
                        when (event) {
                            is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated,
                            is GiniMerchant.MerchantSDKEvents.OnFinishedWithCancellation -> { requireActivity().supportFragmentManager.popBackStack() }
                            is GiniMerchant.MerchantSDKEvents.OnScreenDisplayed -> {
                                if (event.displayedScreen == DisplayedScreen.Nothing) {
                                    requireActivity().supportFragmentManager.popBackStack()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = InvoiceDetailsFragment()
    }
}