@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.ui.components.tooltip

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniTooltipBox(
    tooltipText: String,
    modifier: Modifier = Modifier,
    colors: GiniTooltipBoxColors = GiniTooltipBoxColors.contentDescriptionColors(),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(8.dp),
        tooltip = {
            GiniPlainTooltip(
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    text = tooltipText,
                    style = GiniTheme.typography.body1
                )
            }
        },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
private fun TooltipScope.GiniPlainTooltip(
    modifier: Modifier = Modifier,
    contentColor: Color,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    PlainTooltip(
        modifier = modifier,
        contentColor = contentColor,
        containerColor = containerColor,
        content = content
    )
}
