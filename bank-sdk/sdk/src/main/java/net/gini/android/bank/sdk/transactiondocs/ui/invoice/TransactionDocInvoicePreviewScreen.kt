package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.invoice.colors.InvoicePreviewScreenColors
import net.gini.android.bank.sdk.invoice.colors.section.InvoicePreviewScreenFooterColors
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenu
import net.gini.android.capture.ui.components.menu.context.GiniDropdownMenuItem
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun TransactionDocInvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: TransactionDocInvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()

    InvoiceScreenContent(
        modifier = modifier,
        state = state,
        onCloseClicked = navigateBack,
        colors = colors,
        onDeleteClicked = {
            viewModel.onDeleteClicked()
            navigateBack()
        }
    )
}

private const val DEFAULT_ZOOM = 1f

@Composable
private fun InvoiceScreenContent(
    state: TransactionDocInvoicePreviewFragmentState,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    onDeleteClicked: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddings ->
        var menuVisible by remember { mutableStateOf(false) }
        Box(
            modifier = modifier
                .padding(paddings)
                .fillMaxSize()
                .background(colors.background)
        ) {

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center), visible = state.isLoading
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 64.dp),
                visible = !state.isLoading
            ) {
                ImagesList(
                    modifier = Modifier,
                    pages = state.images
                )
            }

            GiniTopBar(
                title = state.screenTitle,
                colors = colors.topBarColors,
                navigationIcon = {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = onCloseClicked)
                            .padding(start = 16.dp, end = 32.dp),
                        painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
                        contentDescription = null,
                        tint = colors.topBarColors.navigationContentColor
                    )
                },
                actions = {
                    Icon(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                menuVisible = true
                            },
                        painter = painterResource(id = R.drawable.gbs_more_horizontal),
                        tint = colors.topBarColors.actionContentColor,
                        contentDescription = null
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
            )

            Footer(
                modifier = Modifier.align(Alignment.BottomCenter),
                infoTextLines = state.infoTextLines,
                colors = colors.footerColors,
            )
        }
    }
}


@Composable
private fun Footer(
    infoTextLines: List<String>,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenFooterColors,
) {

    Column(
        modifier = modifier
            .background(colors.backgroundColor)
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        infoTextLines.forEach {
            Text(
                text = it,
                style = GiniTheme.typography.caption1,
                color = colors.contentColor
            )
        }
    }
}

@Composable
private fun ImagesList(
    pages: List<Bitmap>,
    modifier: Modifier = Modifier,
    minZoom: Float = DEFAULT_ZOOM,
    onScaleChanged: (Float) -> Unit = {},
) {
    ZoomableLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        onScaleChanged = onScaleChanged,
        minScale = minZoom,
    ) {
        items(pages) {
            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomOut() {
    GiniTheme {
        InvoiceScreenContent(
            state = TransactionDocInvoicePreviewFragmentState(
                screenTitle = "Screen Title",
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
            onDeleteClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomIn() {
    GiniTheme {
        InvoiceScreenContent(
            state = TransactionDocInvoicePreviewFragmentState(
                screenTitle = "Screen Title",
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
            onDeleteClicked = {}
        )
    }
}
