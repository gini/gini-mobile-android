package net.gini.android.bank.sdk.capture.skonto.invoice

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.list.ZoomableLazyColumn
import net.gini.android.capture.ui.theme.GiniTheme
import kotlin.math.abs

@Composable
internal fun SkontoInvoiceScreen(
    navigateBack: () -> Unit,
    viewModel: SkontoInvoiceFragmentViewModel,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors()
) {
    val state by viewModel.stateFlow.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()

    ) { paddings ->
        SkontoInvoiceScreenContent(
            modifier = Modifier.padding(paddings),
            state = state,
            onCloseClicked = navigateBack,
            colors = colors,
        )
    }
}

@Composable
private fun SkontoInvoiceScreenContent(
    state: SkontoInvoiceFragmentState,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SkontoInvoicePreviewScreenColors = SkontoInvoicePreviewScreenColors.colors(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        Box(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 24.dp)
                .background(colors.closeButton.backgroundColor, CircleShape)
                .clickable(onClick = onCloseClicked)
                .padding(8.dp),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = net.gini.android.capture.R.drawable.gc_close),
                contentDescription = null,
                tint = colors.closeButton.contentColor
            )
        }

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
                pages = state.images, modifier = Modifier
            )
        }
    }
}

@Composable
private fun ImagesList(
    pages: List<Bitmap>,
    modifier: Modifier = Modifier,
) {
    ZoomableLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
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
private fun SkontoInvoiceScreenContentPreview() {
    GiniTheme {
        SkontoInvoiceScreenContent(state = SkontoInvoiceFragmentState(
            isLoading = true, images = emptyList()
        ), onCloseClicked = {})
    }
}

