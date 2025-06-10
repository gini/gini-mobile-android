package net.gini.android.capture.internal.qreducation.model

sealed interface FlowType {
    data object Photo : FlowType
    data object QrCode : FlowType
}
