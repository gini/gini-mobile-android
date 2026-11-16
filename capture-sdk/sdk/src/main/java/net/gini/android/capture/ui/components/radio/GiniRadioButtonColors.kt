package net.gini.android.capture.ui.components.radio

import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniRadioButton].
 *
 * @property selectedColor ring and dot when selected
 * @property unselectedColor ring when not selected
 * @property disabledColor ring and dot when disabled
 * @property errorColor ring and dot when the button is in the error state
 */
@Immutable
data class GiniRadioButtonColors(
    val selectedColor: Color = Color.Unspecified,
    val unselectedColor: Color = Color.Unspecified,
    val disabledColor: Color = Color.Unspecified,
    val errorColor: Color = Color.Unspecified,
) {

    @Composable
    internal fun toMaterialRadioButtonColors(isError: Boolean): RadioButtonColors =
        RadioButtonDefaults.colors(
            selectedColor = if (isError) errorColor else selectedColor,
            unselectedColor = if (isError) errorColor else unselectedColor,
            disabledSelectedColor = disabledColor,
            disabledUnselectedColor = disabledColor,
        )

    companion object {
        @Composable
        fun colors(
            selectedColor: Color = GiniTheme.colorScheme.text.accent,
            unselectedColor: Color = GiniTheme.colorScheme.checkbox.box.unchecked,
            disabledColor: Color = GiniTheme.colorScheme.checkbox.box.disabled,
            errorColor: Color = GiniTheme.colorScheme.errorHint.content,
        ) = GiniRadioButtonColors(
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            disabledColor = disabledColor,
            errorColor = errorColor,
        )
    }
}
