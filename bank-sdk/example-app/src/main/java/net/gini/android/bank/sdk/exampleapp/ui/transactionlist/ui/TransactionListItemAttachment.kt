package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Attachment
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun TransactionListItemAttachment(
    attachment: Attachment,
    onClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        modifier = modifier,
        onClick = { onClick(attachment) },
        shape = RoundedCornerShape(24.dp),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DocumentImage(attachment.filename)
                Text(
                    text = attachment.filename,
                    style = GiniTheme.typography.caption1
                )
            }
        }
    )
}

private val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif")

@Composable
private fun DocumentImage(
    documentName: String,
    modifier: Modifier = Modifier,
) {
    val iconResId = if (imageExtensions.find { documentName.endsWith(it, true) } != null) {
        R.drawable.gbs_tl_document_placeholder_image
    } else {
        R.drawable.gbs_tl_document_placeholder_file
    }
    Icon(
        modifier = modifier
            .size(20.dp),
        painter = painterResource(id = iconResId),
        contentDescription = null,
    )
}

@Composable
@GiniScreenPreviewUiModes
private fun TransactionListItemPreview() {
    GiniTheme {
        TransactionListItemAttachment(
            Attachment(
                id = "id", filename = "File Name.pdf",
            ),
            onClick = {}
        )
    }
}
