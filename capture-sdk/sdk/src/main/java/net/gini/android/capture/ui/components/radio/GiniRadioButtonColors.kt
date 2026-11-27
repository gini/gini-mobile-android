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

    /**
     * What to draw for a button in the given state. The error look applies to an enabled button
     * only; a disabled button shows [disabledColor] whatever else is set.
     *
     * @param errorContainer fill behind the outline in the error state
     * @param dot the dot of a selected button outside the error state
     */
    internal fun look(
        selected: Boolean,
        enabled: Boolean,
        isError: Boolean,
        errorContainer: Color,
        dot: Color,
    ): GiniRadioButtonLook {
        val showError = isError && enabled
        val stateColor = when {
            !enabled -> disabledColor
            showError -> errorColor
            selected -> selectedColor
            else -> unselectedColor
        }
        return GiniRadioButtonLook(
            fill = when {
                showError -> errorContainer
                selected -> stateColor
                else -> null
            },
            outline = stateColor.takeIf { showError || !selected },
            dot = when {
                !selected -> null
                showError -> errorColor
                else -> dot
            },
        )
    }

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

/**
 * The parts of a [GiniRadioButton] to draw in one state; `null` leaves a part out.
 *
 * @property fill the disc filling the button
 * @property outline the thin outline around the button
 * @property dot the dot in the middle
 */
internal data class GiniRadioButtonLook(
    val fill: Color?,
    val outline: Color?,
    val dot: Color?,
)
