package net.gini.android.capture.ui.components.checkbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives

data class GiniCheckboxColors(
    val checkedCheckmarkColor: Color,
    val uncheckedCheckmarkColor: Color,
    val checkedBoxColor: Color,
    val uncheckedBoxColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledUncheckedBoxColor: Color,
    val disabledIndeterminateBoxColor: Color,
    val checkedBorderColor: Color,
    val uncheckedBorderColor: Color,
    val disabledBorderColor: Color,
    val disabledUncheckedBorderColor: Color,
    val disabledIndeterminateBorderColor: Color
) {

    companion object {

        @Composable
        fun colors(
            uncheckedCheckmarkColor: Color = GiniTheme.colorScheme.checkbox.checkmark.unchecked,
            uncheckedBorderColor: Color = GiniTheme.colorScheme.checkbox.box.unchecked,
            uncheckedBoxColor: Color = Color.Transparent,
            checkedCheckmarkColor: Color = GiniTheme.colorScheme.checkbox.checkmark.checked,
            checkedBoxColor: Color = GiniTheme.colorScheme.checkbox.box.checked,
            checkedBorderColor: Color = GiniTheme.colorScheme.checkbox.box.checked,
            disabledCheckedBoxColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            disabledUncheckedBoxColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            disabledBorderColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            disabledUncheckedBorderColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            disabledIndeterminateBoxColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            disabledIndeterminateBorderColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
        ) = GiniCheckboxColors(
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
    }
}
