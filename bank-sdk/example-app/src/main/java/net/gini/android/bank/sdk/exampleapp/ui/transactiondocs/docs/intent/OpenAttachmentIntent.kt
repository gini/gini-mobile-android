package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent

import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.TransactionDocsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.TransactionDocsSideEffect
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import javax.inject.Inject

internal class OpenAttachmentIntent @Inject constructor() {

    fun TransactionDocsContainerHost.run(
        attachment: Attachment,
    ) = intent {
        postSideEffect(TransactionDocsSideEffect.OpenTransactionDocInvoiceScreen(attachment.id))
    }
}
