package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent.InitializeIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent.OpenAttachmentIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class TransactionDocsViewModel @Inject constructor(
    private val initializeIntent: InitializeIntent,
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

    fun openAttachment(transaction: Transaction, attachment: Attachment) =
        with(openAttachmentIntent) { run(transaction, attachment) }
}
