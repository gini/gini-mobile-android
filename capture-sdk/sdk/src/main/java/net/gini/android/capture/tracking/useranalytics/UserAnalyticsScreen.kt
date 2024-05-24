package net.gini.android.capture.tracking.useranalytics

import net.gini.android.capture.R

enum class UserAnalyticsScreen(val screenName: String) {
    CAMERA("camera"),
    REVIEW("review"),
    REVIEW_ZOOM("review_zoom"),
    ANALYSIS("analysis"),
    NO_RESULTS("no_results"),
    HELP("help"),
    ERROR("error"),
    ONBOARDING_FLAT_PAPER("onboarding_flat_paper"),
    ONBOARDING_LIGHTING("onboarding_lighting"),
    ONBOARDING_MULTIPLE_PAGES("onboarding_multiple_pages"),
    ONBOARDING_QR_CODE("onboarding_qr_code"),
    ONBOARDING_CUSTOM_1("onboarding_custom_1"),
    ONBOARDING_CUSTOM_2("onboarding_custom_2"),
    ONBOARDING_CUSTOM_3("onboarding_custom_3"),
    ONBOARDING_CUSTOM_4("onboarding_custom_4"),
    ONBOARDING_CUSTOM_5("onboarding_custom_5"),
}


fun getOnboardingScreenNameForUserAnalytics(id: Int): UserAnalyticsScreen {
    return when (id) {
        R.string.gc_onboarding_qr_code_title -> UserAnalyticsScreen.ONBOARDING_QR_CODE
        R.string.gc_onboarding_multipage_title -> UserAnalyticsScreen.ONBOARDING_MULTIPLE_PAGES
        R.string.gc_onboarding_lighting_title -> UserAnalyticsScreen.ONBOARDING_LIGHTING
        R.string.gc_onboarding_align_corners_title -> UserAnalyticsScreen.ONBOARDING_FLAT_PAPER
        0 -> UserAnalyticsScreen.ONBOARDING_CUSTOM_1
        1 -> UserAnalyticsScreen.ONBOARDING_CUSTOM_2
        2 -> UserAnalyticsScreen.ONBOARDING_CUSTOM_3
        3 -> UserAnalyticsScreen.ONBOARDING_CUSTOM_4
        4 -> UserAnalyticsScreen.ONBOARDING_CUSTOM_5
        else -> UserAnalyticsScreen.ONBOARDING_FLAT_PAPER

    }
}
