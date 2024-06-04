package net.gini.android.merchant.sdk.exampleapp.invoices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import net.gini.android.merchant.sdk.exampleapp.databinding.FragmentInvoiceDetailsBinding
import net.gini.android.merchant.sdk.exampleapp.invoices.ui.model.InvoiceItem
import net.gini.android.merchant.sdk.util.setIntervalClickListener


class InvoiceDetailsFragment(invoiceItem: InvoiceItem, private val payButtonListener: (String) -> Unit): Fragment() {

    private lateinit var binding: FragmentInvoiceDetailsBinding
    private val viewModel: InvoiceDetailsViewModel by viewModels { InvoiceDetailsViewModel.Factory(invoiceItem) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentInvoiceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.invoiceNumber.text = viewModel.invoiceItem.documentId
        binding.amount.text = viewModel.invoiceItem.amount
        binding.dueDate.text = viewModel.invoiceItem.dueDate
        binding.payNowBtn.setIntervalClickListener { payButtonListener(viewModel.invoiceItem.documentId) }
    }

    companion object {
        fun newInstance(invoiceItem: InvoiceItem, payButtonListener: (String) -> Unit) = InvoiceDetailsFragment(invoiceItem, payButtonListener)
    }
}