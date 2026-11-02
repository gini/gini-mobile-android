package net.gini.android.capture.ui.components.expandable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * A tappable row with a leading icon, a title and a chevron that reveals [body] when expanded.
 * Used for short explanations the user may want to read ("How do I protect myself?").
 *
 * State is hoisted: the caller owns [expanded] so it survives configuration changes together
 * with the rest of the screen state.
 *
 * The whole row is one accessibility node with the button role; its state is announced as
 * expanded or collapsed.
 *
 * @param title the always visible title
 * @param body the text revealed when expanded
 * @param expanded whether [body] is shown
 * @param onExpandedChange called with the new state when the row is tapped
 * @param icon the leading icon, decorative
 * @param modifier applied to the row
 * @param colors colors of the row, see [GiniExpandableInfoRowColors.colors]
 */
@Composable
fun GiniExpandableInfoRow(
    title: String,
    body: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    colors: GiniExpandableInfoRowColors = GiniExpandableInfoRowColors.colors(),
) {
    val stateDescription = stringResource(
        if (expanded) R.string.gc_a11y_expanded else R.string.gc_a11y_collapsed
    )
    val clickLabel = stringResource(
        if (expanded) R.string.gc_a11y_collapse else R.string.gc_a11y_expand
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_ROTATION_EXPANDED else CHEVRON_ROTATION_COLLAPSED,
        label = "chevronRotation",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(colors.containerColor)
            .clickable(role = Role.Button, onClickLabel = clickLabel) { onExpandedChange(!expanded) }
            .semantics(mergeDescendants = true) { this.stateDescription = stateDescription }
            .heightIn(min = MIN_TOUCH_TARGET)
            .padding(CONTENT_PADDING),
        horizontalArrangement = Arrangement.spacedBy(ICON_TEXT_SPACING),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(ICON_SIZE),
            painter = icon,
            contentDescription = null,
            tint = colors.iconTint,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TITLE_BODY_SPACING),
        ) {
            Text(
                text = title,
                style = GiniTheme.typography.body1,
                color = colors.titleColor,
            )
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = body,
                    style = GiniTheme.typography.caption1,
                    color = colors.bodyColor,
                )
            }
        }
        Icon(
            modifier = Modifier
                .size(CHEVRON_SIZE)
                .rotate(chevronRotation),
            painter = painterResource(R.drawable.gc_chevron_right_icon),
            contentDescription = null,
            tint = colors.chevronTint,
        )
    }
}

private const val CHEVRON_ROTATION_COLLAPSED = 90f
private const val CHEVRON_ROTATION_EXPANDED = -90f
private val CORNER_RADIUS = 12.dp
private val CONTENT_PADDING = 12.dp
private val ICON_SIZE = 24.dp
private val CHEVRON_SIZE = 16.dp
private val ICON_TEXT_SPACING = 12.dp
private val TITLE_BODY_SPACING = 4.dp
private val MIN_TOUCH_TARGET = 48.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniExpandableInfoRowPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GiniExpandableInfoRow(
                    title = "How do I protect myself?",
                    body = "Documents are sometimes altered to redirect payments.",
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    icon = painterResource(R.drawable.gc_shield_icon),
                )
                GiniExpandableInfoRow(
                    title = "How do I protect myself?",
                    body = "Documents are sometimes altered to redirect payments. If in doubt, ask " +
                        "the sender which IBAN is correct, using contact details you already know.",
                    expanded = true,
                    onExpandedChange = {},
                    icon = painterResource(R.drawable.gc_shield_icon),
                )
            }
        }
    }
}
