package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent

import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.TransactionListContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.TransactionListSideEffect
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Attachment
import javax.inject.Inject

internal class OpenAttachmentIntent @Inject constructor() {

    fun TransactionListContainerHost.run(
        attachment: Attachment,
    ) = intent {
        postSideEffect(TransactionListSideEffect.OpenTransactionDocInvoiceScreen(attachment.id))
    }
}
