package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.InvoicePreviewActivity
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.ui.TransactionListItem
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
internal fun TransactionDocsScreen(
    configurationViewModel: ConfigurationViewModel,
    transactionDocsViewModel: TransactionDocsViewModel,
    openTransactionDetails: (Transaction) -> Unit,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by transactionDocsViewModel.collectAsState()

    transactionDocsViewModel.collectSideEffect {
        when (it) {
            is TransactionDocsSideEffect.OpenTransactionDocInvoiceScreen -> {
                // Before Opening a transaction doc invoice GiniBank should be configured
                configurationViewModel.clearGiniCaptureNetworkInstances()
                configurationViewModel.configureGiniBank(context)

                context.startActivity(
                    InvoicePreviewActivity.newIntent(
                        context = context,
                        screenTitle = "Invoice Preview",
                        documentId = it.documentId,
                        infoTextLines = listOf(
                            "Amount to Pay: ${it.transaction.amount}",
                            "IBAN: ${it.transaction.iban}"
                        )
                    )
                )
            }

            is TransactionDocsSideEffect.OpenTransactionDetails ->
                openTransactionDetails(it.transaction)
        }
    }

    Scaffold(
        topBar = {
            GiniTopBar(
                "Photo payment history",
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(net.gini.android.capture.R.drawable.gc_close),
                            contentDescription = null
                        )
                    }

                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn {
                items(state.transactions) { transaction ->
                    TransactionListItem(
                        transaction,
                        onAttachmentClick = {
                            transactionDocsViewModel.openAttachment(transaction, it)
                        },
                        onClick = openTransactionDetails
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
