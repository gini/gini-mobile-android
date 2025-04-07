package net.gini.android.bank.sdk.exampleapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.databinding.FragmentTransactionDocInvoicePreviewContainerBinding

@AndroidEntryPoint
class TransactionDocInvoicePreviewContainerFragment : Fragment() {

    private var _binding: FragmentTransactionDocInvoicePreviewContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDocInvoicePreviewContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val args: TransactionDocInvoicePreviewContainerFragmentArgs by navArgs<TransactionDocInvoicePreviewContainerFragmentArgs>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val documentId = args.documentId

        val infoTextLines: List<String> =
            args.infoTextLines.toList()
        val screenTitle =
            args.screenTitle


        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.transaction_docs_container) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(
            R.navigation.transaction_doc_invoice_preview_nav_graph,
            GiniBank.createTransactionDocInvoicePreviewFragmentArgs(screenTitle, documentId, infoTextLines).toBundle()
        )

    }




}