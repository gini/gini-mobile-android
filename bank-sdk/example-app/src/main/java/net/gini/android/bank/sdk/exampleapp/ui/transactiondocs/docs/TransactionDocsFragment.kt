package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.fragment.ComposeFragment

@AndroidEntryPoint
class TransactionDocsFragment : ComposeFragment() {

    private val viewModel: TransactionDocsViewModel by viewModels()
    private val configurationViewModel: ConfigurationViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        TransactionDocsScreen(
            configurationViewModel = configurationViewModel,
            transactionDocsViewModel = viewModel,
            openTransactionDetails = {
                findNavController().navigate(
                    TransactionDocsFragmentDirections
                        .actionTransactionListDemoToTransactionDetailsFragment(it)
                )
            },
            navigateBack = {
                requireActivity().finish()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.initialize()
    }
}
