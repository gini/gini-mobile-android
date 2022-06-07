package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.DefaultOnboardingIconAdapter

sealed class DefaultPages(val onboardingPage: OnboardingPage) {

    class Page1 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_1_title, R.string.gc_onboarding_page_1_message, DefaultOnboardingIconAdapter(R.drawable.gc_onboarding_page_1)))
    class Page2 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_2_title, R.string.gc_onboarding_page_2_message, DefaultOnboardingIconAdapter(R.drawable.gc_onboarding_page_2)))
    class Page3 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_3_title, R.string.gc_onboarding_page_3_message, DefaultOnboardingIconAdapter(R.drawable.gc_onboarding_page_3)))
    class Page4 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_4_title, R.string.gc_onboarding_page_4_message, DefaultOnboardingIconAdapter(R.drawable.gc_onboarding_page_4)))

    companion object {

        @JvmStatic
        fun asArrayList(isMultiPageEnabled: Boolean): ArrayList<OnboardingPage> {
            val list = mutableListOf(
                Page1().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingAlignCornersIconAdapter != null) {
                        onboardingPage.iconAdapter = GiniCapture.getInstance().onboardingAlignCornersIconAdapter
                    }
                }.onboardingPage,
                Page2().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingLightingIconAdapter != null) {
                        onboardingPage.iconAdapter = GiniCapture.getInstance().onboardingLightingIconAdapter
                    }
                }.onboardingPage
            )
            if (isMultiPageEnabled) {
                list.add(
                    Page3().apply {
                        if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingMultiPageIconAdapter != null) {
                            onboardingPage.iconAdapter = GiniCapture.getInstance().onboardingMultiPageIconAdapter
                        }
                    }.onboardingPage
                )
            }
            list.add(Page4().apply {
                if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingQRCodeIconAdapter != null) {
                    onboardingPage.iconAdapter = GiniCapture.getInstance().onboardingQRCodeIconAdapter
                }
            }.onboardingPage)
            return ArrayList(list)
        }
    }
}
