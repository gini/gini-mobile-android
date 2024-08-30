package net.gini.android.capture.ui.components.menu.context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback.ShapeProvider
import net.gini.android.capture.ui.components.menu.context.colors.GiniContextMenuColors
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    colors: GiniContextMenuColors = GiniContextMenuColors.colors(),
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        modifier = modifier.background(colors.containerColor),
        expanded = expanded,
        offset = offset,
        onDismissRequest = onDismissRequest,
    ) {
        content()
    }
}

@Composable
fun GiniDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: GiniContextMenuColors.ItemColors = GiniContextMenuColors.ItemColors.colors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = colors.textColor,
            leadingIconColor = colors.iconTint,
            trailingIconColor = colors.iconTint,
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

// To see Preview run it in interactive mode and click on empty space
@Composable
@Preview(showBackground = true)
private fun GiniContextMenuPreview() {
    GiniTheme {
        var contextMenuVisible by remember { mutableStateOf(false) }
        Box(modifier = Modifier
            .size(DpSize(150.dp, 100.dp))
            .clickable {
                contextMenuVisible = true
            }) {
            GiniDropdownMenu(
                modifier = Modifier.align(Alignment.Center),
                expanded = contextMenuVisible,
                offset = DpOffset(20.dp, 100.dp),
                onDismissRequest = { contextMenuVisible = false },
            ) {
                GiniDropdownMenuItem(
                    text = {
                        Text(
                            text = "Item 1",
                            style = GiniTheme.typography.body1,
                            color = GiniTheme.colorScheme.text.primary
                        )
                    },
                    onClick = { },
                    leadingIcon = {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Default.Add),
                            contentDescription = null,
                            tint = GiniTheme.colorScheme.icons.secondary
                        )
                    }
                )
                GiniDropdownMenuItem(
                    text = {
                        Text(
                            text = "Item 1",
                            style = GiniTheme.typography.body1,
                            color = GiniTheme.colorScheme.text.primary
                        )
                    },
                    onClick = { },
                    leadingIcon = {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Default.Add),
                            contentDescription = null,
                            tint = GiniTheme.colorScheme.icons.secondary
                        )
                    }
                )
            }
        }
    }
}