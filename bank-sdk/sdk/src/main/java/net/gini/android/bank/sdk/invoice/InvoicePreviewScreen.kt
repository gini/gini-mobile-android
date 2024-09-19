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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.util.currencyFormatterWithoutSymbol
import net.gini.android.bank.sdk.di.getGiniBankKoin
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
private const val INTERFACE_VISIBILITY_ZOOM_THRESHOLD = 1.5f

@Composable
private fun SkontoInvoiceScreenContent(
    state: InvoicePreviewFragmentState,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors(),
    interfaceVisible: Boolean = false,
) {

    var contentZoomScale by remember { mutableFloatStateOf(DEFAULT_ZOOM) }
    var isInterfaceVisible by remember { mutableStateOf(interfaceVisible) }

    LaunchedEffect(contentZoomScale) {
        isInterfaceVisible = contentZoomScale > INTERFACE_VISIBILITY_ZOOM_THRESHOLD
    }

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
                    pages = state.images,
                    onScaleChanged = { contentZoomScale = it }
                )
            }

            AnimatedVisibility(
                modifier = Modifier,
                visible = !isInterfaceVisible
            ) {
                CloseScreenButton(
                    onCloseClicked = onCloseClicked,
                    colors = colors.closeButton,
                )
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = isInterfaceVisible
            ) {
                GiniTopBar(
                    title = stringResource(id = R.string.gbs_skonto_invoice_preview_title),
                    colors = colors.topBarColors,
                    navigationIcon = {
                        Icon(
                            modifier = Modifier
                                .clickable(onClick = onCloseClicked)
                                .padding(horizontal = 8.dp),
                            painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
                            contentDescription = null,
                            tint = colors.topBarColors.navigationContentColor
                        )
                    }
                )
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isInterfaceVisible
            ) {
                Footer(
                    infoTextLines = state.infoTextLines,
                    colors = colors.footerColors,
                )
            }
        }
    }
}

@Composable
private fun CloseScreenButton(
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors.CloseButton,
) {
    Box(
        modifier = modifier
            .padding(vertical = 24.dp, horizontal = 24.dp)
            .background(colors.backgroundColor, CircleShape)
            .clickable(onClick = onCloseClicked)
            .padding(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
            contentDescription = null,
            tint = colors.contentColor
        )
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
            .padding(16.dp)
            .padding(bottom = 16.dp),
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
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
            interfaceVisible = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SkontoInvoiceScreenContentPreviewZoomIn() {
    GiniTheme {
        SkontoInvoiceScreenContent(
            state = InvoicePreviewFragmentState(
                isLoading = true,
                images = emptyList(),
                infoTextLines = listOf("Line 1", "Line 2"),
            ),
            onCloseClicked = {},
            interfaceVisible = true,
        )
    }
}