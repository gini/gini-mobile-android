package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent.DeleteTransactionIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent.InitializeIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent.OpenAttachmentIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class TransactionDocsViewModel @Inject constructor(
    private val initializeIntent: InitializeIntent,
    private val deleteTransactionIntent: DeleteTransactionIntent,
    private val openAttachmentIntent: OpenAttachmentIntent,
) : ViewModel(), TransactionDocsContainerHost {

    override val container = container<TransactionDocsState, TransactionDocsSideEffect>(
        TransactionDocsState(listOf())
    )

    init {
        initialize()
    }

    fun initialize() {
        with(initializeIntent) { run() }
    }

    fun deleteTransaction(transaction: Transaction) =
        with(deleteTransactionIntent) { run(transaction) }

    fun openAttachment(attachment: Attachment) =
        with(openAttachmentIntent) { run(attachment) }
}
