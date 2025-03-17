package net.gini.android.capture.qrengagement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButton
import net.gini.android.capture.ui.components.logo.GiniLogo
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
    BackHandler { }

    val state by viewModel.collectAsState()
    viewModel.collectSideEffect {

    }
}

@Composable
private fun QrEngagementScreenContent(
    state: QrEngagementState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressIndicator(1, 3)
        GiniLogo(modifier = Modifier.align(Alignment.End))
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
) {
    Column(modifier = modifier) {
        Image(painter = imagePainter, contentDescription = null)

        Column {
            Text(text = title)
            Text(text = description)
        }

        Column {
            Row {
                GiniButton(text = "Back", onClick = {})
                GiniButton(text = "Next", onClick = {})
            }
            GiniOutlinedButton(text = "Skip", onClick = {})
        }
    }
}

@Composable
@Preview
private fun ProgressIndicatorPreview() {
    GiniTheme {
        QrEngagementScreenContent(
            state = QrEngagementState(
                isLoading = false
            )
        )
    }
}
