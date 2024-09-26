package net.gini.android.bank.sdk.invoice

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.invoice.colors.SkontoInvoicePreviewScreenColors
import net.gini.android.bank.sdk.invoice.colors.section.SkontoInvoicePreviewScreenFooterColors
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun InvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()

    SkontoInvoiceScreenContent(
        modifier = modifier,
        state = state,
        onCloseClicked = navigateBack,
        colors = colors,
    )
}

private const val DEFAULT_ZOOM = 1f

@Composable
private fun SkontoInvoiceScreenContent(
    state: InvoicePreviewFragmentState,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddings ->
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
    colors: SkontoInvoicePreviewScreenFooterColors,
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
        SkontoInvoiceScreenContent(
            state = InvoicePreviewFragmentState(
                screenTitle = "Screen Title",
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomIn() {
    GiniTheme {
        SkontoInvoiceScreenContent(
            state = InvoicePreviewFragmentState(
                screenTitle = "Screen Title",
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
        )
    }
}
