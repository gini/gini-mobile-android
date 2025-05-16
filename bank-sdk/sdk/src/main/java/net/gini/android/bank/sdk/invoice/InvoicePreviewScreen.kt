package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.components.tooltip.GiniTooltipBox
import net.gini.android.capture.ui.components.topbar.GiniTopBar
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import org.orbitmvi.orbit.compose.collectAsState
import net.gini.android.capture.R as CaptureR

@Composable
internal fun InvoicePreviewScreen(
    navigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    isLandScape : Boolean
) {
    val state by viewModel.collectAsState()

    when (val state = state) {
        is InvoicePreviewFragmentState.Error -> InvoiceScreenErrorContent(
            modifier = modifier,
            onCloseClicked = {
                navigateBack()
                viewModel.onUserNavigatesBack()
            },
            onRetryClicked = viewModel::init,
            errorType = state.errorType
        )

        is InvoicePreviewFragmentState.Ready -> InvoiceScreenReadyContent(
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
            onUserZoomedScreenOnce = viewModel::onUserZoomedImage,
        isLandScape = isLandScape
    )}
}

@Composable
internal fun InvoiceScreenErrorContent(
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    onCloseClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    errorType: ErrorType
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GiniTopBar(
                title = stringResource(id = CaptureR.string.gc_title_error),
                colors = colors.topBarColors.copy(containerColor = GiniColorPrimitives().dark01),
                navigationIcon = {
                    NavigationActionBack(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 32.dp),
                        onClick = onCloseClicked,
                        tint = colors.topBarColors.navigationContentColor
                    )
                },
            )
        }
    ) { paddings ->
        Column(
            modifier = modifier
                .padding(paddings)
                .fillMaxSize()
                .background(colors.background),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                HorizontalDivider(
                    color = colors.errorMessage.errorHint.iconColor.copy(alpha = 0.15f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.errorMessage.errorHint.containerColor)
                        .heightIn(min = 56.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(errorType.drawableResource),
                        contentDescription = null,
                        tint = colors.errorMessage.errorHint.iconColor
                    )
                    Text(
                        text = stringResource(errorType.titleTextResource),
                        style = GiniTheme.typography.body2,
                        color = colors.errorMessage.errorHint.textColor
                    )
                }

                HorizontalDivider(
                    color = colors.errorMessage.errorHint.iconColor.copy(alpha = 0.15f),
                )

                Text(
                    text = stringResource(errorType.descriptionTextResource),
                    style = GiniTheme.typography.body2,
                    color = colors.errorMessage.messageColor,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 16.dp)
                )
            }
            GiniButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                text = stringResource(R.string.gbs_skonto_invoice_preview_try_again), onClick = onRetryClicked
            )
        }
    }
}

private const val DEFAULT_ZOOM = 1f

@Composable
internal fun InvoiceScreenReadyContent(
    isLoading: Boolean,
    screenTitle: String,
    infoTextLines: List<String>,
    images: List<Bitmap>,
    onCloseClicked: () -> Unit,
    onUserZoomedScreenOnce: () -> Unit,
    modifier: Modifier = Modifier,
    colors: InvoicePreviewScreenColors = InvoicePreviewScreenColors.colors(),
    topBarActions: @Composable RowScope.() -> Unit = {},
    isLandScape: Boolean = false
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
                    },
                    isLandScape = isLandScape
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
                painter = painterResource(id = CaptureR.drawable.gc_close),
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
    isLandScape: Boolean
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
                contentScale =
                    if (isLandScape)
                        ContentScale.FillHeight
                    else ContentScale.FillWidth
            )
        }
    }
}

@Preview(name = "Landscape", device = "spec:width=891dp,height=411dp", showBackground = true)
@Composable
private fun InvoiceScreenContentPreviewLandscape() {
    GiniTheme {
        InvoiceScreenReadyContent(
            onCloseClicked = {},
            screenTitle = "Screen Title",
            isLoading = true,
            images = emptyList(),
            infoTextLines = listOf("Line 1", "Line 2"),
            onUserZoomedScreenOnce = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceScreenContentPreview() {
    GiniTheme {
        InvoiceScreenReadyContent(
            onCloseClicked = {},
            screenTitle = "Screen Title",
            isLoading = true,
            images = emptyList(),
            infoTextLines = listOf("Line 1", "Line 2"),
            onUserZoomedScreenOnce = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceScreenErrorContentPreview() {
    GiniTheme {
        InvoiceScreenErrorContent(
            onCloseClicked = {},
            onRetryClicked = {},
            errorType = ErrorType.GENERAL
        )
    }
}
