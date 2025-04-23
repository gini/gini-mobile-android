package net.gini.android.bank.sdk.exampleapp.ui.extractions.intent

import kotlinx.coroutines.flow.firstOrNull
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionDocsStorage
import net.gini.android.bank.sdk.exampleapp.ui.extractions.ExtractionsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import java.util.UUID
import javax.inject.Inject

internal class SaveTransactionDataIntent @Inject constructor(
    private val transactionDocsStorage: TransactionDocsStorage,
) {

    fun ExtractionsContainerHost.run(
        iban: String,
        bic: String,
        amountToPay: String,
        paymentRecipient: String,
        paymentPurpose: String,
        paymentReference: String,
    ) = blockingIntent {

        val list = transactionDocsStorage.get() ?: emptyList()
        val attachments =
            GiniBank.giniTransactionDocs.giniTransactionDocsFlow.firstOrNull() ?: emptyList()

        transactionDocsStorage.update(
            list + Transaction(
                id = UUID.randomUUID().toString(),
                paymentRecipient = paymentRecipient,
                paymentPurpose = paymentPurpose,
                amount = amountToPay,
                attachments = listOf(
                    attachments.map {
                        Attachment(
                            id = it.giniApiDocumentId,
                            filename = it.documentFileName
                        )
                    }
                ).firstOrNull() ?: emptyList(),
                iban = iban,
                bic = bic,
                timestamp = System.currentTimeMillis(),
                paymentReference = paymentReference,
            )
        )
    }
}
