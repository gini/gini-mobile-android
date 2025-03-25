package net.gini.android.capture.qrengagement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButton
import net.gini.android.capture.ui.components.logo.GiniLogo
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
internal fun QrEngagementScreen(
    viewModel: QrEngagementViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        viewModel.onNavigateBackClicked()
    }

    val state by viewModel.collectAsState()

    viewModel.collectSideEffect {
        when(it) {
            QrEngagementSideEffect.NavigateBack -> navController.popBackStack()
            QrEngagementSideEffect.Skip -> TODO()
        }
    }

    QrEngagementScreenContent(
        modifier = modifier,
        state = state
    )
}

@Composable
private fun QrEngagementScreenContent(
    state: QrEngagementState,
    modifier: Modifier = Modifier,
) {

    val title = when (state.page) {
        QrEngagementState.Page.NotJustQrCodes -> stringResource(R.string.gc_qr_engagement_page_1_title)
        QrEngagementState.Page.PhotosPdfsMore -> stringResource(R.string.gc_qr_engagement_page_2_title)
        QrEngagementState.Page.EvenScreensGifs -> stringResource(R.string.gc_qr_engagement_page_3_title)
    }

    val description = when (state.page) {
        QrEngagementState.Page.NotJustQrCodes -> stringResource(R.string.gc_qr_engagement_page_1_description)
        QrEngagementState.Page.PhotosPdfsMore -> stringResource(R.string.gc_qr_engagement_page_2_description)
        QrEngagementState.Page.EvenScreensGifs -> stringResource(R.string.gc_qr_engagement_page_3_description)
    }

    val painter = when (state.page) {
        QrEngagementState.Page.NotJustQrCodes -> painterResource(R.drawable.gc_qr_code_engagement_page_1)
        QrEngagementState.Page.PhotosPdfsMore -> painterResource(R.drawable.gc_qr_code_engagement_page_2)
        QrEngagementState.Page.EvenScreensGifs -> painterResource(R.drawable.gc_qr_code_engagement_page_3)
    }

    val progress = when (state.page) {
        QrEngagementState.Page.NotJustQrCodes -> 1
        QrEngagementState.Page.PhotosPdfsMore -> 2
        QrEngagementState.Page.EvenScreensGifs -> 3
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressIndicator(progress, 3)
        GiniLogo(modifier = Modifier.align(Alignment.End))
        QrEngagementPageContent(
            imagePainter = painter,
            title = title,
            description = description,
            isReadMoreButtonVisible = state.page == QrEngagementState.Page.NotJustQrCodes
        )
    }
}

@Composable
private fun ProgressIndicator(
    progress: Int,
    maxProgress: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GiniColorPrimitives.buildColorPrimitivesBasedOnResources(LocalContext.current)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$progress / $maxProgress")
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(maxProgress) {
                ProgressIndicatorItem(
                    modifier = modifier.weight(1f),
                    color = if (it + 1 == progress) {
                        colors.accent01
                    } else {
                        Color(0x4D121212)
                    }
                )
            }
        }
    }

}

@Composable
private fun ProgressIndicatorItem(
    color: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .height(4.dp)
            .fillMaxWidth()
            .background(
                color = color,
                shape = RoundedCornerShape(100)
            )
    )
}

@Composable
private fun QrEngagementPageContent(
    imagePainter: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    isReadMoreButtonVisible: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterHorizontally),
            painter = imagePainter,
            contentDescription = null
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = GiniTheme.typography.headline6
            )
            Text(
                text = description,
                style = GiniTheme.typography.body1
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedVisibility(isReadMoreButtonVisible) {
                GiniButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.gc_qr_engagement_btn_read_more),
                    onClick = {})
            }
            AnimatedVisibility(
                visible = !isReadMoreButtonVisible
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GiniButton(
                        modifier = Modifier.weight(0.65f),
                        text = stringResource(R.string.gc_qr_engagement_btn_back),
                        onClick = {}
                    )
                    GiniButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.gc_qr_engagement_btn_next),
                        onClick = {}
                    )
                }
            }
            GiniOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.gc_qr_engagement_btn_skip),
                onClick = {}
            )
        }
    }
}

@Composable
@GiniScreenPreviewUiModes
private fun ProgressIndicatorPreview() {
    GiniTheme {
        Surface {
            QrEngagementScreenContent(
                state = QrEngagementState(
                    page = QrEngagementState.Page.NotJustQrCodes
                )
            )
        }
    }
}
