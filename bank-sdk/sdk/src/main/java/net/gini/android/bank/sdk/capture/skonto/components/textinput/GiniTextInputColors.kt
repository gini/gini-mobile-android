package net.gini.android.bank.sdk.capture.skonto.components.textinput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.ui.theme.GiniTheme

@Immutable
data class GiniTextInputColors(
    val containerFocused: Color,
    val containerUnfocused: Color,
    val containerDisabled: Color,
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
            containerFocused: Color = GiniTheme.colorScheme.textField.containerFocused,
            containerUnfocused: Color = GiniTheme.colorScheme.textField.containerUnfocused,
            containerDisabled: Color = GiniTheme.colorScheme.textField.containerDisabled,
            textFocused: Color = GiniTheme.colorScheme.textField.textFocused,
            textUnfocused: Color = GiniTheme.colorScheme.textField.textUnfocused,
            textDisabled: Color = GiniTheme.colorScheme.textField.textDisabled,
            textError: Color = GiniTheme.colorScheme.textField.textError,
            indicatorFocused: Color = GiniTheme.colorScheme.textField.indicatorFocused,
            indicatorUnfocused: Color = GiniTheme.colorScheme.textField.indicatorUnfocused,
            indicatorDisabled: Color = GiniTheme.colorScheme.textField.indicatorDisabled,
            indicatorError: Color = GiniTheme.colorScheme.textField.indicatorError,
            trailingContentFocused: Color = GiniTheme.colorScheme.textField.trailingContentFocused,
            trailingContentUnfocused: Color = GiniTheme.colorScheme.textField.trailingContentUnfocused,
            trailingContentDisabled: Color = GiniTheme.colorScheme.textField.trailingContentDisabled,
            trailingContentError: Color = GiniTheme.colorScheme.textField.trailingContentError,
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
        )
    }
}