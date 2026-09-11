package net.gini.android.bank.sdk.capture.digitalinvoice

import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize

/**
 * Created by Alpar Szotyori on 17.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * The `SelectableLineItem` wrapps a [LineItem] and adds the possibility to select/deselect it.
 */
@Parcelize
class SelectableLineItem(
    var selected: Boolean = true,
    var addedByUser: Boolean = false,
    val lineItem: LineItem
) : Parcelable {

    override fun toString() = "LineItem(selected=$selected, addedByUser=$addedByUser, lineItem=$lineItem)"

    override fun equals(other: Any?) = other is SelectableLineItem
            && selected == other.selected
            && addedByUser == other.addedByUser
            && lineItem == other.lineItem

    override fun hashCode() = Objects.hash(selected, addedByUser, lineItem)

    @JvmSynthetic
    fun copy(
        selected: Boolean = this.selected,
        lineItem: LineItem = this.lineItem
    ) = SelectableLineItem(
        selected, addedByUser,
        lineItem.copy()
    )
}
