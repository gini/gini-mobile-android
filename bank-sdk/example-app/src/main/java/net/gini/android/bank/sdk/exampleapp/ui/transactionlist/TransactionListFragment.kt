package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.gini.android.bank.sdk.exampleapp.ui.fragment.ComposeFragment

@AndroidEntryPoint
class TransactionListFragment : ComposeFragment() {

    private val viewModel: TransactionListViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        TransactionListScreen(
            transactionListViewModel = viewModel
        )
    }
}