package net.gini.android.capture.ui.components.picker.date

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniDatePickerDialogColors(
    val dateSelected: Color = Color.Unspecified,
    val borderDate: Color = Color.Unspecified,
    val textHeadline: Color = Color.Unspecified,
    val textSupporting: Color = Color.Unspecified,
    val textButtons: Color = Color.Unspecified,
    val textDateToday: Color = Color.Unspecified,
    val textDateSelected: Color = Color.Unspecified,
    val textDateEnabled: Color = Color.Unspecified,
    val textDateDisabled: Color = Color.Unspecified,
    val divider: Color = Color.Unspecified,
) {

    companion object {

        @Composable
        fun colors(
            dateSelected: Color = GiniTheme.colorScheme.datePicker.date.containerFocused,
            borderDate: Color = GiniTheme.colorScheme.datePicker.date.containerOutlined,
            textHeadline: Color = GiniTheme.colorScheme.datePicker.text.primary,
            textSupporting: Color = GiniTheme.colorScheme.datePicker.text.secondary,
            textButtons: Color = GiniTheme.colorScheme.datePicker.text.accent,
            textDateToday: Color = GiniTheme.colorScheme.datePicker.text.primary,
            textDateSelected: Color = GiniTheme.colorScheme.datePicker.date.contentFocused,
            textDateEnabled: Color = GiniTheme.colorScheme.datePicker.text.primary,
            textDateDisabled: Color = GiniTheme.colorScheme.datePicker.text.secondary,
            divider: Color = GiniTheme.colorScheme.datePicker.divider,
        ) = GiniDatePickerDialogColors(
            dateSelected = dateSelected,
            borderDate = borderDate,
            textHeadline = textHeadline,
            textSupporting = textSupporting,
            textButtons = textButtons,
            textDateToday = textDateToday,
            textDateSelected = textDateSelected,
            textDateEnabled = textDateEnabled,
            textDateDisabled = textDateDisabled,
            divider = divider,
        )
    }
}