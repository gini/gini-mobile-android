package net.gini.android.bank.sdk.capture.skonto.mapper

import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

internal fun SkontoEdgeCase.toAnalyticsModel() = when(this) {
    SkontoEdgeCase.PayByCashOnly -> UserAnalyticsEventProperty.EdgeCaseType.Type.PayByCash
    SkontoEdgeCase.PayByCashToday -> UserAnalyticsEventProperty.EdgeCaseType.Type.PayByCash
    SkontoEdgeCase.SkontoExpired -> UserAnalyticsEventProperty.EdgeCaseType.Type.Expired
    SkontoEdgeCase.SkontoLastDay -> UserAnalyticsEventProperty.EdgeCaseType.Type.PayToday
}
