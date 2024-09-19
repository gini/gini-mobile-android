package net.gini.android.bank.sdk.transactiondocs.internal.provider.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc.AttachDocumentToTransactionDialog
import net.gini.android.capture.analysis.transactiondoc.AttachDocToTransactionDialogProvider
import net.gini.android.capture.util.compose.collectFlow

internal class AttachDocToTransactionDialogProviderImpl(
    private val giniTransactionDocsSettings: GiniTransactionDocsSettings,
) : AttachDocToTransactionDialogProvider {

    // TODO Refactor shortcut for hiding dialog later
    override fun provide(
        onDismiss: () -> Unit,
        onConfirm: (alwaysAttach: Boolean) -> Unit,
    ): @Composable () -> Unit {
        return {
            var dialogVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (giniTransactionDocsSettings.getAlwaysAttachSetting().first()) {
                    onConfirm(true)
                } else {
                    dialogVisible = true
                }
            }
            AnimatedVisibility(visible = dialogVisible) {
                AttachDocumentToTransactionDialog(
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }
}
