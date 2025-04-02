package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Attachment
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Transaction
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenu
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenuItem
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.typography.bold

@Composable
internal fun TransactionListItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onAttachmentClick: (Attachment) -> Unit,
    onDeleteClicked: () -> Unit,
) {
    var menuVisible by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = transaction.title,
                style = GiniTheme.typography.subtitle1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = transaction.amount,
                    style = GiniTheme.typography.subtitle1.bold()
                )

                Icon(
                    modifier = Modifier
                        .rotate(90f)
                        .clickable {
                            menuVisible = true
                        },
                    painter = painterResource(net.gini.android.bank.sdk.R.drawable.gbs_more_horizontal),
                    contentDescription = null
                )

                if (menuVisible) {
                    GiniDropdownMenu(
                        expanded = true,
                        onDismissRequest = { menuVisible = false },
                    ) {
                        GiniDropdownMenuItem(
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                menuVisible = false
                                onDeleteClicked()
                            },
                            text = {
                                Text("Delete")
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = transaction.description,
            style = GiniTheme.typography.caption1
        )
        transaction.attachments.firstOrNull()?.let {
            TransactionListItemAttachment(
                it,
                onClick = onAttachmentClick
            )
        }
    }
}

@Composable
@GiniScreenPreviewUiModes
private fun TransactionListItemPreview() {
    GiniTheme {
        Surface {
            TransactionListItem(
                Transaction(
                    title = "Some title",
                    description = "Description",
                    amount = "197 EUR",
                    attachments = listOf(
                        Attachment(
                            id = "id", filename = "File Name.pdf"
                        )
                    )
                ),
                onAttachmentClick = {},
                onDeleteClicked = {}
            )
        }
    }
}
