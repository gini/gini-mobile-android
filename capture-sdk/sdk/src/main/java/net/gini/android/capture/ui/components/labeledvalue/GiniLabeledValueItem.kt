package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.runtime.Immutable

/**
 * The texts of one [GiniLabeledValue], for screens that build their rows as data first.
 *
 * @property label the caption above the value
 * @property value the value itself
 * @property hint the optional line below the value
 * @property valueContentDescription what accessibility services read in place of [value]
 */
@Immutable
data class GiniLabeledValueItem(
    val label: String,
    val value: String,
    val hint: String? = null,
    val valueContentDescription: String? = null,
)
