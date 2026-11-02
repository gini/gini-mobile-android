package net.gini.android.capture.ui.components.radio

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * A radio button in the Gini style.
 *
 * Pass `null` as [onClick] when the surrounding row handles the selection (with
 * `Modifier.selectable`), so the row is the single accessibility target.
 *
 * @param selected whether this option is the chosen one
 * @param onClick called when the button itself is tapped, or `null` to leave clicks to the parent
 * @param modifier applied to the button
 * @param enabled whether the button reacts to input
 * @param isError draws the button in the error color, e.g. when a choice is required but missing
 * @param colors colors, see [GiniRadioButtonColors.colors]
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
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors.toMaterialRadioButtonColors(isError),
        interactionSource = interactionSource,
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniRadioButtonPreview() {
    GiniTheme {
        Column(modifier = Modifier.padding(8.dp)) {
            GiniRadioButton(selected = true, onClick = {})
            GiniRadioButton(selected = false, onClick = {})
            GiniRadioButton(selected = false, onClick = {}, isError = true)
            GiniRadioButton(selected = true, onClick = {}, enabled = false)
        }
    }
}
