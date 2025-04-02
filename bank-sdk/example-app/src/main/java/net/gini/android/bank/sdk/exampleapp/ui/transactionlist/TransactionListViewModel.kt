package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent.DeleteTransactionIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent.InitializeIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent.OpenAttachmentIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Transaction
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class TransactionListViewModel @Inject constructor(
    initializeIntent: InitializeIntent,
    private val deleteTransactionIntent: DeleteTransactionIntent,
    private val openAttachmentIntent: OpenAttachmentIntent,
) : ViewModel(), TransactionListContainerHost {

    override val container = container<TransactionListState, TransactionListSideEffect>(
        TransactionListState(listOf())
    )

    init {
        with(initializeIntent) { run() }
    }

    fun deleteTransaction(transaction: Transaction) =
        with(deleteTransactionIntent) { run(transaction) }

    fun openAttachment(attachment: Attachment) =
        with(openAttachmentIntent) { run(attachment) }
}
