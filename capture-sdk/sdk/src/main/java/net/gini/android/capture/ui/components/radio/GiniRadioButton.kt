package net.gini.android.capture.ui.components.radio

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniSheetColors

/**
 * A radio button in the Gini style: an outlined circle that becomes a filled disc with a dot in
 * its middle when selected.
 *
 * Material's [RadioButton] supplies the behavior (semantics, touch target, ripple) and is made
 * invisible; the Gini look is drawn behind it, centered like Material's own ring.
 *
 * Pass `null` as [onClick] when the surrounding row handles the selection (with
 * `Modifier.selectable`), so the row is the single accessibility target.
 *
 * @param selected whether this option is the chosen one
 * @param onClick called when the button itself is tapped, or `null` to leave clicks to the parent
 * @param modifier applied to the button
 * @param enabled whether the button reacts to input
 * @param isError draws the button in the error color, with the error container color behind
 * the outline while it is not selected, e.g. when a choice is required but missing
 * @param colors colors, see [GiniRadioButtonColors.colors]; a [Color.Unspecified] one falls
 * back to its theme default
 * @param interactionSource the interaction source observed for pressed/focused states
 */
@Composable
fun GiniRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    colors: GiniRadioButtonColors = GiniRadioButtonColors.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val look = colors.orDefaults().look(
        selected = selected,
        enabled = enabled,
        isError = isError,
        errorContainer = GiniSheetColors.radioButtonErrorContainer,
        dot = GiniSheetColors.radioButtonDot,
    )
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier.drawBehind {
            val radius = RADIUS.toPx()
            val borderWidth = BORDER_WIDTH.toPx()
            look.fill?.let { drawCircle(color = it, radius = radius) }
            look.outline?.let {
                drawCircle(
                    color = it,
                    radius = radius - borderWidth / 2,
                    style = Stroke(width = borderWidth),
                )
            }
            look.dot?.let { drawCircle(color = it, radius = DOT_RADIUS.toPx()) }
        },
        enabled = enabled,
        colors = RadioButtonDefaults.colors(
            selectedColor = Color.Transparent,
            unselectedColor = Color.Transparent,
            disabledSelectedColor = Color.Transparent,
            disabledUnselectedColor = Color.Transparent,
        ),
        interactionSource = interactionSource,
    )
}

// A 24dp circle, the size Material gives the button when the row handles the clicks.
private val RADIUS = 12.dp
private val DOT_RADIUS = 6.dp
private val BORDER_WIDTH = 1.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
internal fun GiniRadioButtonPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(modifier = Modifier.padding(8.dp)) {
                GiniRadioButton(selected = true, onClick = {})
                GiniRadioButton(selected = false, onClick = {})
                GiniRadioButton(selected = false, onClick = {}, isError = true)
                GiniRadioButton(selected = true, onClick = {}, enabled = false)
            }
        }
    }
}
