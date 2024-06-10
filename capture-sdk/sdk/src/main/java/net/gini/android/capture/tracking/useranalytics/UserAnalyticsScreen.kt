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
    object OnboardingFlatPaper : UserAnalyticsScreen("onboarding_flat_paper")
    object OnboardingLighting : UserAnalyticsScreen("onboarding_lighting")
    object OnboardingMultiplePages : UserAnalyticsScreen("onboarding_multiple_pages")
    object OnboardingQrCode : UserAnalyticsScreen("onboarding_qr_code")
    object ReturnAssistant : UserAnalyticsScreen("return_assistant")
    object EditReturnAssistant : UserAnalyticsScreen("edit_return_assistant")
    data class OnboardingCustom(val page: Int) : UserAnalyticsScreen("onboarding_custom_$page")
}
