package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent.InitializeIntent
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class TransactionListViewModel @Inject constructor(
    private val initializeIntent: InitializeIntent,
) : ViewModel(), TransactionListContainerHost {

    override val container = container<TransactionListState, TransactionListSideEffect>(
        TransactionListState(emptyList())
    )

    init {
        with(initializeIntent) { run() }
    }
}