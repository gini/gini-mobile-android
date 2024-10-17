package net.gini.android.capture.ui.components.button.filled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    giniButtonColors: GiniButtonColors = GiniButtonColors(),
) {
    GiniButton(
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
fun GiniButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    giniButtonColors: GiniButtonColors = GiniButtonColors.colors(),
    content: @Composable () -> Unit,
) {
    Button(
        modifier = modifier.padding(vertical = 16.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
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
            GiniButton(text = "Text Only", onClick = {})
        }
    }
}

@Preview
@Composable
private fun GiniContentButtonPreview() {
    GiniTheme {
        Column {
            GiniButton(onClick = {}) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Outlined.Done),
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