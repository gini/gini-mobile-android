package net.gini.android.capture.qrengagement

sealed interface QrEngagementSideEffect {
    data object NavigateBack : QrEngagementSideEffect
    data object Skip : QrEngagementSideEffect
}
