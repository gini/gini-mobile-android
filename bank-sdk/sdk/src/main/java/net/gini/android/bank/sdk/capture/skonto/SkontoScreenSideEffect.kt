package net.gini.android.bank.sdk.capture.skonto

internal sealed interface SkontoScreenSideEffect {
    data class OpenInvoiceScreen(
        val documentId: String,
        val infoTextLines: List<String>,
    ) : SkontoScreenSideEffect

    object OpenHelpScreen : SkontoScreenSideEffect
    object NavigateBack : SkontoScreenSideEffect
}
