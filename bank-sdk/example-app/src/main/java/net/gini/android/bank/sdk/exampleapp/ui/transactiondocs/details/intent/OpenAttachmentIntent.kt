package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.intent

import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.TransactionDetailsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.TransactionDetailsSideEffect
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import javax.inject.Inject

internal class OpenAttachmentIntent @Inject constructor() {

    fun TransactionDetailsContainerHost.run(
        attachment: Attachment,
    ) = intent {
        postSideEffect(
            TransactionDetailsSideEffect.OpenTransactionDocInvoiceScreen(
                state.transaction,
                attachment.id
            )
        )
    }
}
