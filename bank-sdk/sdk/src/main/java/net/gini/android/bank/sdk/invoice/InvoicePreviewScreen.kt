package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import net.gini.android.bank.sdk.invoice.colors.InvoicePreviewScreenColors
import net.gini.android.bank.sdk.invoice.colors.section.InvoicePreviewScreenFooterColors
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
internal fun InvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()

    InvoiceScreenContent(
        modifier = modifier,
        onCloseClicked = navigateBack,
        colors = colors,
        isLoading = state.isLoading,
        screenTitle = state.screenTitle,
        infoTextLines = state.infoTextLines,
        images = state.images
    )
}

private const val DEFAULT_ZOOM = 1f

@Composable
internal fun InvoiceScreenContent(
    isLoading: Boolean,
    screenTitle: String,
    infoTextLines: List<String>,
    images: List<Bitmap>,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    topBarActions: @Composable RowScope.() -> Unit = {},
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
                modifier = Modifier.align(Alignment.Center), visible = isLoading
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 64.dp),
                visible = !isLoading
            ) {
                ImagesList(
                    modifier = Modifier,
                    pages = images
                )
            }

            GiniTopBar(
                title = screenTitle,
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
                actions = topBarActions,
            )

            Footer(
                modifier = Modifier.align(Alignment.BottomCenter),
                infoTextLines = infoTextLines,
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
private fun InvoiceScreenContentPreview() {
    GiniTheme {
        InvoiceScreenContent(
            onCloseClicked = {},
            screenTitle = "Screen Title",
            isLoading = true,
            images = emptyList(),
            infoTextLines = listOf("Line 1", "Line 2"),
        )
    }
}