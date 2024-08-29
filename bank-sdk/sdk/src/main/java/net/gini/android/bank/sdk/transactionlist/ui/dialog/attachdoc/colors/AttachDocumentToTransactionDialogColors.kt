package net.gini.android.bank.sdk.transactionlist.ui.dialog.attachdoc.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.checkbox.GiniCheckboxColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class AttachDocumentToTransactionDialogColors(
    val containerColor: Color,
    val headerIconColor: Color,
    val titleColor: Color,
    val contentColor: Color,
    val buttonTextColor: Color,
    val checkableContentColors: CheckableContentColors,
) {

    @Immutable
    data class CheckableContentColors(
        val checkboxColor: GiniCheckboxColors,
        val textColor: Color,
    ) {
        companion object {
            @Composable
            fun colors(
                checkboxColor: GiniCheckboxColors = GiniCheckboxColors.colors(),
                textColor: Color = GiniTheme.colorScheme.dialogs.text,
            ) = CheckableContentColors(
                checkboxColor = checkboxColor,
                textColor = textColor,
            )
        }
    }

    companion object {

        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.dialogs.container,
            headerIconColor: Color = GiniTheme.colorScheme.dialogs.text,
            titleColor: Color = GiniTheme.colorScheme.dialogs.text,
            contentColor: Color = GiniTheme.colorScheme.dialogs.text,
            buttonTextColor: Color = GiniTheme.colorScheme.dialogs.labelText,
            checkableContentColors: CheckableContentColors = CheckableContentColors.colors(),
        ) = AttachDocumentToTransactionDialogColors(
            containerColor = containerColor,
            headerIconColor = headerIconColor,
            titleColor = titleColor,
            contentColor = contentColor,
            buttonTextColor = buttonTextColor,
            checkableContentColors = checkableContentColors,
        )
    }
}
