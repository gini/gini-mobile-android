package net.gini.android.bank.sdk.analytics

import net.gini.android.bank.sdk.capture.digitalinvoice.LineItem
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventProperty.ItemsChanged.DifferenceType

internal fun LineItem.getDifferences(compareTo: LineItem?): Set<DifferenceType> {

    compareTo ?: return emptySet()

    return setOfNotNull(
        DifferenceType.Quantity.takeIf { this.quantity != compareTo.quantity },
        DifferenceType.Name.takeIf { this.description != compareTo.description },
        DifferenceType.Price.takeIf { this.grossPrice != compareTo.grossPrice }
    )
}