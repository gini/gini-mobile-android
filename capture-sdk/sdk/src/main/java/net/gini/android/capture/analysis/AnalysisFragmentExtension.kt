package net.gini.android.capture.analysis

import androidx.compose.ui.platform.ComposeView
import net.gini.android.capture.analysis.transactiondoc.AttachDocToTransactionDialogProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.ui.theme.GiniTheme

open class AnalysisFragmentExtension {

    private val attachDocToTransactionDialogProvider: AttachDocToTransactionDialogProvider
            by getGiniCaptureKoin().inject()

    fun showAttachDocToTransactionDialog(
        composeView: ComposeView,
        onDismiss: () -> Unit,
        onConfirm: (alwaysAttach: Boolean) -> Unit
    ) {
        composeView.setContent {
            GiniTheme {
                attachDocToTransactionDialogProvider.provide(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                ).invoke()
            }
        }
    }
}
