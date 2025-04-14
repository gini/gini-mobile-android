package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.intent

import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionDocsStorage
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.TransactionDetailsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import javax.inject.Inject

internal class DeleteAttachmentIntent @Inject constructor(
    private val transactionDocsStorage: TransactionDocsStorage
) {

    fun TransactionDetailsContainerHost.run(
        attachment: Attachment,
    ) = intent {
        val transactions = transactionDocsStorage.get() ?: emptyList()
        val updatedTransaction = state.transaction.copy(
            attachments = state.transaction.attachments.filter { it.id != attachment.id }
        )

        transactionDocsStorage.update(
            transactions.map {
                if (it == state.transaction) {
                    updatedTransaction
                } else {
                    it
                }
            }
        )

        reduce { state.copy(transaction = updatedTransaction) }
    }
}
