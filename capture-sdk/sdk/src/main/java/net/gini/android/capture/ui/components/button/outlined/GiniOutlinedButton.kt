package net.gini.android.capture.ui.components.button.outlined

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    giniButtonColors: GiniOutlinedButtonColors = GiniOutlinedButtonColors.colors(),
) {
    GiniOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        giniButtonColors = giniButtonColors,
    ) {
        Text(
            text = text,
        )
    }
}

@Composable
fun GiniOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    giniButtonColors: GiniOutlinedButtonColors = GiniOutlinedButtonColors.colors(),
    content: @Composable () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        // gc_dark_06 matches the grey of Material's default outline, which no SDK color
        // resource reaches. Read here rather than added to GiniOutlinedButtonColors: that class
        // is released API, and a new field would break integrators' binaries.
        border = BorderStroke(BORDER_WIDTH, GiniTheme.primitives.dark06),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = giniButtonColors.container,
            contentColor = giniButtonColors.content
        ),
    ) {
        // The SDK's Button text style (Root.GiniCaptureTheme.Typography.Button), so the label
        // follows the integrator's typography instead of Material's labelLarge.
        ProvideTextStyle(GiniTheme.typography.button) {
            content()
        }
    }
}

private val BORDER_WIDTH = 1.dp


@GiniScreenPreviewUiModes
@Composable
private fun GiniTextButtonPreview() {
    GiniTheme {
        Column {
            GiniOutlinedButton(text = "Text Only", onClick = {})
        }
    }
}

@GiniScreenPreviewUiModes
@Composable
private fun GiniContentButtonPreview() {
    GiniTheme {
        Column {
            GiniOutlinedButton(onClick = {}) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.gc_close),
                        contentDescription = null
                    )
                    Text(
                        text = "Custom Content",
                    )
                }
            }
        }
    }
}
