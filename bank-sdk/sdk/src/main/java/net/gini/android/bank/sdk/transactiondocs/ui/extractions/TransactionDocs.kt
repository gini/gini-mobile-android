package net.gini.android.bank.sdk.transactiondocs.ui.extractions

import android.content.res.Configuration
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
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.colors.TransactionDocsWidgetColors
import net.gini.android.bank.sdk.transactionlist.internal.GiniBankTransactionDocs
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun TransactionDocs(
    modifier: Modifier = Modifier,
    colors: TransactionDocsWidgetColors = TransactionDocsWidgetColors.colors()
) {
    val transactionDocs: GiniBankTransactionDocs? = remember { GiniBank.giniBankTransactionDocs }

    Card(
        modifier = modifier,
        shape = RectangleShape
    ) {
        if (transactionDocs == null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Transaction Docs feature is not configured",
                style = GiniTheme.typography.headline6,
            )
        } else {
            val documents by transactionDocs.extractionDocumentsFlow.collectAsState()
            TransactionDocsContent(
                documents = documents,
                colors = colors,
                onDocumentDelete = {
                    transactionDocs.updateExtractionDocuments(documents.minus(it))
                },
                onDocumentClick = {
                    
                }
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

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TransactionListDocumentsPreviewNight() {
    GiniTheme {
        TransactionDocs()
    }
}