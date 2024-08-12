package net.gini.android.capture.ui.components.textinput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniTextInputColors(
    val containerFocused: Color,
    val containerUnfocused: Color,
    val containerDisabled: Color,
    val labelError: Color,
    val labelUnfocused: Color,
    val labelFocused: Color,
    val labelDisabled: Color,
    val textFocused: Color,
    val textUnfocused: Color,
    val textDisabled: Color,
    val textError: Color,
    val indicatorFocused: Color,
    val indicatorUnfocused: Color,
    val indicatorDisabled: Color,
    val indicatorError: Color,
    val trailingContentFocused: Color,
    val trailingContentUnfocused: Color,
    val trailingContentDisabled: Color,
    val trailingContentError: Color,
) {
    companion object {

        @Composable
        fun colors(
            containerFocused: Color = GiniTheme.colorScheme.textField.container,
            containerUnfocused: Color = GiniTheme.colorScheme.textField.container,
            containerDisabled: Color = GiniTheme.colorScheme.textField.container,
            textFocused: Color = GiniTheme.colorScheme.textField.text.focused,
            textUnfocused: Color = GiniTheme.colorScheme.textField.text.unfocused,
            textDisabled: Color = GiniTheme.colorScheme.textField.text.disabled,
            textError: Color = GiniTheme.colorScheme.textField.text.focused,
            indicatorFocused: Color = GiniTheme.colorScheme.textField.indicator.focused,
            indicatorUnfocused: Color = GiniTheme.colorScheme.textField.indicator.unfocused,
            indicatorDisabled: Color = GiniTheme.colorScheme.textField.indicator.disabled,
            indicatorError: Color = GiniTheme.colorScheme.textField.indicator.error,
            trailingContentFocused: Color = GiniTheme.colorScheme.textField.content.trailing,
            trailingContentUnfocused: Color = GiniTheme.colorScheme.textField.content.trailing,
            trailingContentDisabled: Color = GiniTheme.colorScheme.textField.content.trailing,
            trailingContentError: Color = GiniTheme.colorScheme.textField.content.trailing,
            labelError: Color = GiniTheme.colorScheme.textField.label.error,
            labelUnfocused: Color = GiniTheme.colorScheme.textField.label.unfocused,
            labelFocused: Color = GiniTheme.colorScheme.textField.label.focused,
            labelDisabled: Color = GiniTheme.colorScheme.textField.label.disabled,
        ) = GiniTextInputColors(
            containerFocused = containerFocused,
            containerUnfocused = containerUnfocused,
            containerDisabled = containerDisabled,
            textFocused = textFocused,
            textUnfocused = textUnfocused,
            textDisabled = textDisabled,
            textError = textError,
            indicatorFocused = indicatorFocused,
            indicatorUnfocused = indicatorUnfocused,
            indicatorDisabled = indicatorDisabled,
            indicatorError = indicatorError,
            trailingContentFocused = trailingContentFocused,
            trailingContentUnfocused = trailingContentUnfocused,
            trailingContentDisabled = trailingContentDisabled,
            trailingContentError = trailingContentError,
            labelError = labelError,
            labelUnfocused = labelUnfocused,
            labelFocused = labelFocused,
            labelDisabled = labelDisabled,
        )
    }
}