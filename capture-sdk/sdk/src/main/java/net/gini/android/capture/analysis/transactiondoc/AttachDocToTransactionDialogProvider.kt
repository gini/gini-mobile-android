package net.gini.android.capture.analysis.transactiondoc

import androidx.compose.runtime.Composable

interface AttachDocToTransactionDialogProvider {
    fun provide(
        onDismiss: () -> Unit,
        onConfirm: (alwaysAttach: Boolean) -> Unit,
    ): @Composable () -> Unit
}
