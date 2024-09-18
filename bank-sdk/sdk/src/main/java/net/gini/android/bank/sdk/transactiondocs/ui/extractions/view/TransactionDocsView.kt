package net.gini.android.bank.sdk.transactiondocs.ui.extractions.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import net.gini.android.bank.sdk.transactiondocs.ui.extractions.TransactionDocs
import net.gini.android.capture.ui.theme.GiniTheme

class TransactionDocsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        val composeView = ComposeView(context, attrs)
        composeView.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(composeView)

        composeView.setContent {
            GiniTheme {
                TransactionDocs()
            }
        }
    }
}