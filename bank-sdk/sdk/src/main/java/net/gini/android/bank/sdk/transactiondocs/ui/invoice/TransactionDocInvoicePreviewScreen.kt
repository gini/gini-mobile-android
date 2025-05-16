package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.invoice.InvoiceScreenErrorContent
import net.gini.android.bank.sdk.invoice.InvoiceScreenReadyContent
import net.gini.android.bank.sdk.invoice.colors.InvoicePreviewScreenColors
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenu
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenuItem
import net.gini.android.capture.ui.theme.GiniTheme
import org.orbitmvi.orbit.compose.collectAsState

@Composable
internal fun TransactionDocInvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: TransactionDocInvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    isLandScape : Boolean
) {
    val state by viewModel.collectAsState()

    when (val state = state) {
        is TransactionDocInvoicePreviewFragmentState.Error -> InvoiceScreenErrorContent(
            modifier = modifier,
            onCloseClicked = {
                navigateBack()
            },
            onRetryClicked = viewModel::init,
            errorType = state.errorType,
        )

        is TransactionDocInvoicePreviewFragmentState.Ready -> InvoiceScreenReadyContent(
        modifier = modifier,
        onCloseClicked = navigateBack,
        colors = colors,
        topBarActions = {
            TransactionDocTopBarActions(
                onDeleteClicked = {
                    viewModel.onDeleteClicked()
                    navigateBack()
                },
                colors = colors
            )
        },
        infoTextLines = state.infoTextLines,
        images = state.images,
        screenTitle = state.screenTitle,
        isLoading = state.isLoading,
        onUserZoomedScreenOnce = { /* No Action needed on TD */ },
        isLandScape = isLandScape
    )
}


}

@Composable
private fun TransactionDocTopBarActions(
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors()
) {
    var menuVisible by remember { mutableStateOf(false) }
    Icon(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable {
                menuVisible = true
            },
        painter = painterResource(id = R.drawable.gbs_more_horizontal),
        tint = colors.topBarColors.actionContentColor,
        contentDescription = stringResource(
            id = R.string.gbs_td_invoice_preview_content_description_more
        )
    )
    if (menuVisible) {
        GiniDropdownMenu(
            colors = colors.topBarOverflowMenuColors,
            expanded = true,
            onDismissRequest = { menuVisible = false },
        ) {
            GiniDropdownMenuItem(
                modifier = modifier,
                text = {
                    Text(
                        text = stringResource(
                            id =
                            R.string.gbs_td_extraction_result_documents_section_menu_delete
                        ),
                        style = GiniTheme.typography.body1,
                        color = colors.topBarOverflowMenuColors.itemColors.textColor
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.gbs_delete),
                        contentDescription = null,
                        tint = colors.topBarOverflowMenuColors.itemColors.textColor
                    )
                },
                onClick = onDeleteClicked
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceScreenContentPreview() {
    GiniTheme {
        InvoiceScreenReadyContent(
            screenTitle = "Screen Title",
            isLoading = true,
            images = emptyList(),
            infoTextLines = listOf("Line 1", "Line 2"),
            onCloseClicked = {},
            onUserZoomedScreenOnce = {}
        )
    }
}
