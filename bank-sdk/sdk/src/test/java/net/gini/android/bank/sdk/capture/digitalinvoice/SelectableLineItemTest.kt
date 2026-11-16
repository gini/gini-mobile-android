package net.gini.android.bank.sdk.capture.digitalinvoice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [SelectableLineItem]: the return reason used to be part of its state, its
 * `toString`, `equals` and `copy`. Now it only wraps the selection state and the line item.
 */
class SelectableLineItemTest {

    private val lineItem = LineItem(
        id = "id1",
        description = "Shoes",
        quantity = 2,
        rawGrossPrice = "12.00:EUR"
    )

    @Test
    fun `toString describes selection, addedByUser and the line item`() {
        val selectableLineItem = SelectableLineItem(selected = false, addedByUser = true, lineItem = lineItem)

        assertThat(selectableLineItem.toString())
            .isEqualTo("LineItem(selected=false, addedByUser=true, lineItem=$lineItem)")
    }

    @Test
    fun `equals compares selection, addedByUser and the line item`() {
        val first = SelectableLineItem(selected = true, addedByUser = false, lineItem = lineItem)
        val same = SelectableLineItem(selected = true, addedByUser = false, lineItem = lineItem)
        val deselected = SelectableLineItem(selected = false, addedByUser = false, lineItem = lineItem)
        val addedByUser = SelectableLineItem(selected = true, addedByUser = true, lineItem = lineItem)

        assertThat(first).isEqualTo(same)
        assertThat(first).isNotEqualTo(deselected)
        assertThat(first).isNotEqualTo(addedByUser)
    }

    @Test
    fun `copy keeps addedByUser and copies the line item`() {
        val original = SelectableLineItem(selected = true, addedByUser = true, lineItem = lineItem)

        val copy = original.copy(selected = false)

        assertThat(copy.selected).isFalse()
        assertThat(copy.addedByUser).isTrue()
        assertThat(copy.lineItem).isEqualTo(lineItem)
        assertThat(copy.lineItem).isNotSameInstanceAs(lineItem)
    }
}
