package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.InvoicePreviewActivity
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.mapper.toTransactionDoc
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.formatter.DateFormatter
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.TransactionDocsContent
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
internal fun TransactionDetailsScreen(
    configurationViewModel: ConfigurationViewModel,
    viewModel: TransactionDetailsViewModel,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.collectAsState()

    BackHandler {
        navigateBack()
    }

    viewModel.collectSideEffect {
        when (it) {
            is TransactionDetailsSideEffect.OpenTransactionDocInvoiceScreen -> {
                // Before Opening a transaction doc invoice GiniBank should be configured
                configurationViewModel.clearGiniCaptureNetworkInstances()
                configurationViewModel.configureGiniBank(context)

                context.startActivity(
                    InvoicePreviewActivity.newIntent(
                        context = context,
                        screenTitle = "Invoice Preview",
                        documentId = it.documentId,
                        infoTextLines = listOf()
                    )
                )
            }
        }
    }

    val dateFormatter = remember { DateFormatter() }

    val fields = listOf(
        "Transaction Date" to { dateFormatter.format(state.transaction.timestamp) },
        "Amount" to { state.transaction.amount.replace(":", " ") },
        "Payment Recipient" to { state.transaction.paymentRecipient },
        "Payment Purpose" to { state.transaction.paymentPurpose },
        "Payment Reference" to { state.transaction.paymentReference },
        "IBAN" to { state.transaction.iban },
        "BIC" to { state.transaction.bic }
    )

    Scaffold(
        topBar = {
            GiniTopBar(
                "Transaction Details",
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(net.gini.android.capture.R.drawable.gc_action_bar_back),
                            contentDescription = null
                        )
                    }

                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(vertical = 8.dp)) {
            LazyColumn {
                items(fields) {
                    Field(placeholder = it.first, text = it.second())
                }
            }
            TransactionDocsContent(
                modifier = Modifier.padding(16.dp),
                documents = state.transaction.attachments.map { it.toTransactionDoc() },
                onDocumentClick = { doc ->
                    state.transaction.attachments.firstOrNull { it.id == doc.giniApiDocumentId }?.let {
                        viewModel.openAttachment(it)
                    }
                },
                onDocumentDelete = { doc ->
                    state.transaction.attachments.firstOrNull { it.id == doc.giniApiDocumentId }?.let {
                        viewModel.deleteAttachment(it)
                    }
                }
            )
        }
    }


}

@Composable
private fun Field(
    placeholder: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    GiniTextInput(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        text = text,
        onValueChange = {},
        label = {
            Text(placeholder)
        },
        readOnly = true
    )
}
