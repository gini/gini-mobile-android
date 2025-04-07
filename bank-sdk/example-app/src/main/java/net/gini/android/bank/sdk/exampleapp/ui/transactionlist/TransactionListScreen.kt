package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.InvoicePreviewActivity
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.ui.TransactionListItem
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.typography.bold
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
internal fun TransactionListScreen(
    configurationViewModel: ConfigurationViewModel,
    transactionListViewModel: TransactionListViewModel,
) {
    val context = LocalContext.current
    val state by transactionListViewModel.collectAsState()

    transactionListViewModel.collectSideEffect {
        when (it) {
            is TransactionListSideEffect.OpenTransactionDocInvoiceScreen -> {
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

    Column {
        Text(
            modifier = Modifier.padding(top = 48.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
            text = "Photo payment history",
            style = GiniTheme.typography.headline6.bold()
        )
        LazyColumn {
            items(state.transactions) {
                TransactionListItem(
                    it,
                    onAttachmentClick = {
                        transactionListViewModel.openAttachment(it)
                    }, onDeleteClicked = {
                        transactionListViewModel.deleteTransaction(it)
                    })
                HorizontalDivider()
            }
        }
    }
}
