package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniLabeledValue] and [GiniLabeledValueList].
 *
 * @property labelColor color of the small label above the value
 * @property valueColor color of the value
 * @property hintColor color of the optional hint line below the value
 * @property titleColor color of the list title
 * @property dividerColor color of the hairline between list items
 */
@Immutable
data class GiniLabeledValueColors(
    val labelColor: Color = Color.Unspecified,
    val valueColor: Color = Color.Unspecified,
    val hintColor: Color = Color.Unspecified,
    val titleColor: Color = Color.Unspecified,
    val dividerColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            labelColor: Color = GiniTheme.colorScheme.text.secondary,
            valueColor: Color = GiniTheme.colorScheme.text.primary,
            hintColor: Color = GiniTheme.colorScheme.errorHint.content,
            titleColor: Color = GiniTheme.colorScheme.text.primary,
            dividerColor: Color = GiniTheme.colorScheme.dialogs.borderColor,
        ) = GiniLabeledValueColors(
            labelColor = labelColor,
            valueColor = valueColor,
            hintColor = hintColor,
            titleColor = titleColor,
            dividerColor = dividerColor,
        )
    }
}
