package net.gini.android.bank.sdk.transactiondocs.ui.extractions.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import net.gini.android.bank.sdk.transactiondocs.model.extractions.TransactionDoc
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.TransactionDocs
import net.gini.android.capture.ui.theme.GiniTheme

class TransactionDocsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var openInvoicePreview: (doc: TransactionDoc, infoTextLines: List<String>) -> Unit =
        { _, _ -> }

    init {
        val composeView = ComposeView(context, attrs)
        composeView.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(composeView)


        composeView.setContent {
            val onDocumentClick by remember { mutableStateOf(openInvoicePreview) }

            GiniTheme {
                TransactionDocs(
                    onDocumentClick = onDocumentClick
                )
            }
        }
    }

    fun onDocumentClick(action: (doc: TransactionDoc, infoTextLines: List<String>) -> Unit) {
        openInvoicePreview = action
    }
}
