package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniLabeledValue].
 *
 * @property labelColor color of the small label above the value
 * @property valueColor color of the value
 * @property hintColor color of the optional hint line below the value
 */
@Immutable
data class GiniLabeledValueColors(
    val labelColor: Color = Color.Unspecified,
    val valueColor: Color = Color.Unspecified,
    val hintColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            labelColor: Color = GiniTheme.colorScheme.text.secondary,
            valueColor: Color = GiniTheme.colorScheme.text.primary,
            hintColor: Color = GiniTheme.colorScheme.errorHint.content,
        ) = GiniLabeledValueColors(
            labelColor = labelColor,
            valueColor = valueColor,
            hintColor = hintColor,
        )
    }
}
