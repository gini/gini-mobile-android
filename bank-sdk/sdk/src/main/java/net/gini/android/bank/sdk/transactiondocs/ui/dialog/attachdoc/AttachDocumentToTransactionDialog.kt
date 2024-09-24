package net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.TransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc.colors.AttachDocumentToTransactionDialogColors
import net.gini.android.capture.ui.components.checkbox.GiniCheckbox
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun AttachDocumentToTransactionDialog(
    giniTransactionDocsSettings: GiniTransactionDocsSettings,
    onDismiss: () -> Unit,
    onConfirm: (alwaysAttach: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: AttachDocumentToTransactionDialogColors = AttachDocumentToTransactionDialogColors.colors(),
) {

    var alwaysAttachChecked by remember { mutableStateOf(true) }

    LaunchedEffect(alwaysAttachChecked) {
        giniTransactionDocsSettings.setAlwaysAttachSetting(alwaysAttachChecked)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.containerColor,
                contentColor = colors.contentColor
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    painter = painterResource(id = R.drawable.gbs_note_stack),
                    contentDescription = null,
                    tint = colors.headerIconColor,
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.gbs_td_attach_document_dialog_title),
                    style = GiniTheme.typography.headline5,
                    color = colors.titleColor,
                    textAlign = TextAlign.Center,
                )
                Column {
                    Text(
                        text = stringResource(id = R.string.gbs_td_attach_document_dialog_content),
                        style = GiniTheme.typography.body2,
                        color = colors.contentColor,
                    )
                    AlwaysAttachCheckableText(
                        modifier = Modifier.padding(vertical = 4.dp),
                        checked = alwaysAttachChecked,
                        onCheckedChange = { alwaysAttachChecked = !alwaysAttachChecked },
                        colors = colors.checkableContentColors
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        modifier = Modifier.padding(start = 52.dp),
                        onClick = { onDismiss() }) {
                        Text(
                            text = stringResource(id = R.string.gbs_td_attach_document_dialog_cancel_button_text),
                            style = GiniTheme.typography.subtitle2
                        )
                    }
                    TextButton(
                        onClick = {
                            onConfirm(alwaysAttachChecked)
                        }) {
                        Text(
                            text = stringResource(id = R.string.gbs_td_attach_document_dialog_confirm_button_text),
                            style = GiniTheme.typography.subtitle2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlwaysAttachCheckableText(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: AttachDocumentToTransactionDialogColors.CheckableContentColors =
        AttachDocumentToTransactionDialogColors.CheckableContentColors.colors()
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GiniCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = colors.checkboxColor
        )
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(id = R.string.gbs_td_attach_document_dialog_always_attach_text),
            style = GiniTheme.typography.subtitle1,
            color = colors.textColor
        )
    }
}

@Preview
@Composable
fun AttachDocumentToTransactionDialogPreview() {
    PreviewContent()
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AttachDocumentToTransactionDialogPreviewDark() {
    PreviewContent()
}

@Composable
private fun PreviewContent() {
    GiniTheme {
        val ctx = LocalContext.current
        AttachDocumentToTransactionDialog(
            onDismiss = {},
            onConfirm = {},
            giniTransactionDocsSettings = GiniTransactionDocsSettings(ctx)
        )
    }
}
