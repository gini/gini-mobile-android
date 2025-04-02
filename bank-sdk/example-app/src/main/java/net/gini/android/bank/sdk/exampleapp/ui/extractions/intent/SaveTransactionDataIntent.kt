package net.gini.android.bank.sdk.exampleapp.ui.extractions.intent

import kotlinx.coroutines.flow.firstOrNull
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionListStorage
import net.gini.android.bank.sdk.exampleapp.ui.extractions.ExtractionsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Transaction
import javax.inject.Inject

internal class SaveTransactionDataIntent @Inject constructor(
    private val transactionListStorage: TransactionListStorage,
) {

    fun ExtractionsContainerHost.run(
        amountToPay: String,
        paymentRecipient: String,
        paymentPurpose: String,
    ) = intent {
        val list = transactionListStorage.get() ?: emptyList()
        val attachments =
            GiniBank.transactionDocs.extractionDocumentsFlow.firstOrNull() ?: emptyList()

        transactionListStorage.update(
            list + Transaction(
                title = paymentRecipient,
                description = paymentPurpose,
                amount = amountToPay,
                attachments = listOf(
                    attachments.map {
                        Attachment(
                            id = it.giniApiDocumentId,
                            filename = it.documentFileName
                        )
                    }
                ).firstOrNull() ?: emptyList()
            )
        )
    }
}
