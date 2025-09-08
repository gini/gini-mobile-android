package net.gini.android.capture.ui.components.button.filled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniButton(
    text: String,
    onClick: () -> Unit,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
    modifier: Modifier = Modifier,
    giniButtonColors: GiniButtonColors = GiniButtonColors(),
) {
    GiniButton(
        onClick = onClick,
        modifier = modifier,
        giniButtonColors = giniButtonColors,
        composableProviderConfig = composableProviderConfig
    ) {
        Text(
            text = text,
        )
    }
}

@Composable
private fun GiniButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
    giniButtonColors: GiniButtonColors = GiniButtonColors.colors(),
    content: @Composable () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = composableProviderConfig?.primaryButtonStyle?.shape ?: RoundedCornerShape(4.dp),
        elevation = composableProviderConfig?.primaryButtonStyle?.elevation,
        border = composableProviderConfig?.primaryButtonStyle?.border,
        colors = composableProviderConfig?.primaryButtonStyle?.colors ?: ButtonDefaults.buttonColors(
            containerColor = giniButtonColors.containerColor,
            contentColor = giniButtonColors.contentContent,
        ),
    ) {
        content()
    }
}


@Preview
@Composable
private fun GiniTextButtonPreview() {
    GiniTheme {
        Column {
            GiniButton(text = "Text Only", onClick = {}, composableProviderConfig = GiniComposableStyleProviderConfig())
        }
    }
}

@Preview
@Composable
private fun GiniContentButtonPreview() {
    GiniTheme {
        Column {
            GiniButton(onClick = {}, composableProviderConfig = GiniComposableStyleProviderConfig()) {
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