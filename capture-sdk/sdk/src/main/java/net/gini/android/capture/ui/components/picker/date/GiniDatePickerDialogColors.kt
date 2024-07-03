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
    val textMenu: Color = Color.Unspecified,
    val divider: Color = Color.Unspecified,
    val iconMenu: Color = Color.Unspecified,
    val iconButton: Color = Color.Unspecified,
) {

    companion object {

        @Composable
        fun colors(
            dateSelected: Color = GiniTheme.colorScheme.datePicker.dateSelected,
            borderDate: Color = GiniTheme.colorScheme.datePicker.borderDate,
            textHeadline: Color = GiniTheme.colorScheme.datePicker.textHeadline,
            textSupporting: Color = GiniTheme.colorScheme.datePicker.textSupporting,
            textButtons: Color = GiniTheme.colorScheme.datePicker.textButtons,
            textDateToday: Color = GiniTheme.colorScheme.datePicker.textDateToday,
            textDateSelected: Color = GiniTheme.colorScheme.datePicker.textDateSelected,
            textDateEnabled: Color = GiniTheme.colorScheme.datePicker.textDateEnabled,
            textDateDisabled: Color = GiniTheme.colorScheme.datePicker.textDateDisabled,
            textMenu: Color = GiniTheme.colorScheme.datePicker.textMenu,
            divider: Color = GiniTheme.colorScheme.datePicker.divider,
            iconMenu: Color = GiniTheme.colorScheme.datePicker.iconMenu,
            iconButton: Color = GiniTheme.colorScheme.datePicker.iconButton,
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
            textMenu = textMenu,
            divider = divider,
            iconMenu = iconMenu,
            iconButton = iconButton,
        )
    }
}