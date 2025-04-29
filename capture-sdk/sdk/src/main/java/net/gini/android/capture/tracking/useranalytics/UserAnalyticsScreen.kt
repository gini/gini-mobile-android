package net.gini.android.capture.tracking.useranalytics

sealed class UserAnalyticsScreen(val name: String) {
    object Camera : UserAnalyticsScreen("camera")
    object CameraAccess : UserAnalyticsScreen("camera_access")
    object Review : UserAnalyticsScreen("review")
    object ReviewZoom : UserAnalyticsScreen("review_zoom")
    object Analysis : UserAnalyticsScreen("analysis")
    object NoResults : UserAnalyticsScreen("no_results")
    object Help : UserAnalyticsScreen("help")
    object Error : UserAnalyticsScreen("error")
    sealed class OnBoarding(name: String) : UserAnalyticsScreen(name) {
        object FlatPaper : OnBoarding("onboarding_flat_paper")
        object Lighting : OnBoarding("onboarding_lighting")
        object MultiplePages : OnBoarding("onboarding_multiple_pages")
        object QrCode : OnBoarding("onboarding_qr_code")
        data class Custom(val page: Int) : UserAnalyticsScreen("onboarding_custom_$page")
    }

    object ReturnAssistant : UserAnalyticsScreen("return_assistant")
    object ReturnAssistantOnBoarding : UserAnalyticsScreen("onoarding_return_assistant")

    object EditReturnAssistant : UserAnalyticsScreen("edit_return_assistant")
    object Skonto : UserAnalyticsScreen("skonto")
    object SkontoReturnAssistant : UserAnalyticsScreen("return_assistant_skonto")
    object SkontoInvoicePreview : UserAnalyticsScreen("skonto_invoice_preview")
}
