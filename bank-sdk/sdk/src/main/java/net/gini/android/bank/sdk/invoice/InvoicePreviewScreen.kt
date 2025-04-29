package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import net.gini.android.capture.ui.components.tooltip.GiniTooltipBox
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme
import org.orbitmvi.orbit.compose.collectAsState

@Composable
internal fun InvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
) {
    val state by viewModel.collectAsState()

    InvoiceScreenContent(
        modifier = modifier,
        onCloseClicked = {
            navigateBack()
            viewModel.onUserNavigatesBack()
        },
        colors = colors,
        isLoading = state.isLoading,
        screenTitle = state.screenTitle,
        infoTextLines = state.infoTextLines,
        images = state.images,
        onUserZoomedScreenOnce = viewModel::onUserZoomedImage
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
    onUserZoomedScreenOnce: () -> Unit,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    topBarActions: @Composable RowScope.() -> Unit = {},
) {
    var isUserZoomedOnce = false
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
                    pages = images,
                    onScaleChanged = {
                        if (it != DEFAULT_ZOOM && !isUserZoomedOnce) {
                            onUserZoomedScreenOnce()
                            isUserZoomedOnce = true
                        }
                    }
                )
            }

            GiniTopBar(
                title = screenTitle,
                colors = colors.topBarColors,
                navigationIcon = {
                    NavigationActionBack(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 32.dp),
                        onClick = onCloseClicked,
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
private fun NavigationActionBack(
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    GiniTooltipBox(
        tooltipText = stringResource(
            id = R.string.gbs_skonto_screen_content_description_close
        )
    ) {
        IconButton(
            modifier = modifier
                .width(24.dp)
                .height(24.dp),
            onClick = onClick
        ) {
            Icon(
                painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
                contentDescription = stringResource(
                    id = R.string.gbs_skonto_screen_content_description_close
                ),
                tint = tint
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
            onUserZoomedScreenOnce = {}
        )
    }
}
