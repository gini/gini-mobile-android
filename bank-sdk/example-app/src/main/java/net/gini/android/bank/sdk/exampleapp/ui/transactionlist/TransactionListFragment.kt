package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.fragment.ComposeFragment

@AndroidEntryPoint
class TransactionListFragment : ComposeFragment() {

    private val viewModel: TransactionListViewModel by viewModels()
    private val configurationViewModel : ConfigurationViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        TransactionListScreen(
            configurationViewModel = configurationViewModel,
            transactionListViewModel = viewModel
        )
    }
}
