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
import net.gini.android.merchant.sdk.exampleapp.databinding.FragmentInvoiceDetailsBinding
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
        viewModel.startObservingPaymentFlow()
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
                    viewModel.finishPaymentFlow.collect {
                        if (it == true) {
                            requireActivity().supportFragmentManager.popBackStack()
                            viewModel.resetFinishPaymentFlow()
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