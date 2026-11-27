package net.gini.android.capture.ui.components.radio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniRadioButton].
 *
 * @property selectedColor the filled disc when selected
 * @property unselectedColor outline when not selected
 * @property disabledColor outline, or the filled disc when selected, when disabled
 * @property errorColor outline, and the dot when selected, in the error state
 */
@Immutable
data class GiniRadioButtonColors(
    val selectedColor: Color = Color.Unspecified,
    val unselectedColor: Color = Color.Unspecified,
    val disabledColor: Color = Color.Unspecified,
    val errorColor: Color = Color.Unspecified,
) {

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
