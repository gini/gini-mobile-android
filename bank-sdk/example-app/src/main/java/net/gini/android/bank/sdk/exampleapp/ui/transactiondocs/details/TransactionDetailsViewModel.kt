package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.intent.DeleteAttachmentIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.intent.InitializeIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.intent.OpenAttachmentIntent
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
internal class TransactionDetailsViewModel @Inject constructor(
    state: SavedStateHandle,
    initializeIntent: InitializeIntent,
    private val openAttachmentIntent: OpenAttachmentIntent,
    private val deleteAttachmentIntent: DeleteAttachmentIntent,
) : ViewModel(), TransactionDetailsContainerHost {

    override val container = container<TransactionDetailsState, TransactionDetailsSideEffect>(
        TransactionDetailsState(
            TransactionDetailsFragmentArgs.fromSavedStateHandle(state).transaction
        )
    )

    init {
        with(initializeIntent) { run() }
    }

    fun deleteAttachment(attachment: Attachment) =
        with(deleteAttachmentIntent) { run(attachment) }

    fun openAttachment(attachment: Attachment) =
        with(openAttachmentIntent) { run(attachment) }
}
