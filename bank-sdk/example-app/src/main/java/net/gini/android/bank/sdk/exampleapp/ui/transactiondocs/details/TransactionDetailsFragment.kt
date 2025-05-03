package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.fragment.ComposeFragment

@AndroidEntryPoint
class TransactionDetailsFragment : ComposeFragment() {

    private val viewModel: TransactionDetailsViewModel by viewModels()
    private val configurationViewModel: ConfigurationViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        TransactionDetailsScreen(
            configurationViewModel = configurationViewModel,
            viewModel = viewModel,
            navigateBack = { findNavController().navigateUp() }
        )
    }
}
