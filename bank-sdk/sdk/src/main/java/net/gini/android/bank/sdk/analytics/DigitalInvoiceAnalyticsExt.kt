package net.gini.android.bank.sdk.analytics

import net.gini.android.bank.sdk.capture.digitalinvoice.LineItem


private const val FIELD_NAME = "name"
private const val FIELD_QUANTITY = "quantity"
private const val FIELD_PRICE = "price"
internal fun LineItem.getDifferences(compareTo: LineItem?): Set<String> {

    compareTo ?: return emptySet()

    return setOfNotNull(
        FIELD_QUANTITY.takeIf { this.quantity != compareTo.quantity },
        FIELD_NAME.takeIf { this.description != compareTo.description },
        FIELD_PRICE.takeIf { this.grossPrice != compareTo.grossPrice }
    )
}