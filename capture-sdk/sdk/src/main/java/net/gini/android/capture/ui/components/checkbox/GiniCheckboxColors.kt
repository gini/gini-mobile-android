package net.gini.android.capture.ui.components.checkbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

        // TODO
        @Composable
        fun colors(
            uncheckedCheckmarkColor: Color = GiniColorPrimitives().light01,
            uncheckedBorderColor: Color = Color(0xFF49454F),
            uncheckedBoxColor: Color = Color.Transparent,

            checkedCheckmarkColor: Color = GiniColorPrimitives().light01,
            checkedBoxColor: Color = GiniColorPrimitives().accent01,
            checkedBorderColor: Color = GiniColorPrimitives().accent01,

            disabledCheckedBoxColor: Color = Color.Gray,
            disabledUncheckedBoxColor: Color = Color.Gray,
            disabledIndeterminateBoxColor: Color = Color.Gray,
            disabledBorderColor: Color = Color.Gray,
            disabledUncheckedBorderColor: Color = Color.Gray,
            disabledIndeterminateBorderColor: Color = Color.Gray,
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
