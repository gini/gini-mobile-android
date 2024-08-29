package net.gini.android.capture.ui.components.checkbox

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    colors: GiniCheckboxColors = GiniCheckboxColors.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
) {
    Checkbox(
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = colors.toMaterialCheckboxColors(),
    )
}

@Preview(showBackground = true)
@Composable
fun GiniCheckboxCheckedPreview() {
    GiniTheme {
        GiniCheckbox(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GiniCheckboxUncheckedPreview() {
    GiniTheme {
        GiniCheckbox(
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GiniCheckboxCheckedDisabledPreview() {
    GiniTheme {
        GiniCheckbox(
            checked = true,
            enabled = false,
            onCheckedChange = {},
        )
    }
}

private fun GiniCheckboxColors.toMaterialCheckboxColors() = CheckboxColors(
    checkedCheckmarkColor = checkedCheckmarkColor,
    uncheckedCheckmarkColor = uncheckedCheckmarkColor,
    checkedBoxColor = checkedBoxColor,
    uncheckedBoxColor = uncheckedBoxColor,
    disabledCheckedBoxColor = disabledCheckedBoxColor,
    disabledUncheckedBoxColor = disabledUncheckedBoxColor,
    disabledIndeterminateBoxColor = disabledIndeterminateBoxColor,
    checkedBorderColor = checkedBorderColor,
    uncheckedBorderColor = uncheckedBorderColor,
    disabledBorderColor = disabledBorderColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledIndeterminateBorderColor = disabledIndeterminateBorderColor,
)
