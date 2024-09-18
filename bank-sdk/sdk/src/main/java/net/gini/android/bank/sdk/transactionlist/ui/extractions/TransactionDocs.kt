package net.gini.android.bank.sdk.transactionlist.ui.extractions

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.transactionlist.ui.extractions.colors.TransactionDocsWidgetColors
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun TransactionDocs(
    modifier: Modifier = Modifier,
    colors: TransactionDocsWidgetColors = TransactionDocsWidgetColors.colors()
) {
    val transactionList = remember { GiniBank.getGiniBankTransactionList() }

    Card(
        modifier = modifier,
        shape = RectangleShape
    ) {
        if (transactionList == null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Transaction list feature is not configured",
                style = GiniTheme.typography.headline6,
            )
        } else {
            val documents by transactionList.extractionDocumentsFlow.collectAsState()
            TransactionDocumentsWidgetContent(
                documents = documents,
                colors = colors
            )
        }
    }
}

@Composable
@Preview
private fun TransactionListDocumentsPreview() {
    GiniTheme {
        TransactionDocs()
    }
}