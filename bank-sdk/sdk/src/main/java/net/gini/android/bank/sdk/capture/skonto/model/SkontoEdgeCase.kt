package net.gini.android.bank.sdk.capture.skonto.model

internal sealed class SkontoEdgeCase {
    object SkontoLastDay : SkontoEdgeCase()
    object PayByCashOnly : SkontoEdgeCase()
    object SkontoExpired : SkontoEdgeCase()
}