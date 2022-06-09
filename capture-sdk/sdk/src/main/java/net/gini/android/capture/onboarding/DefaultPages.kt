package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.DefaultOnboardingIllustrationAdapter

sealed class DefaultPages(val onboardingPage: OnboardingPage) {

    class Page1 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_1_title, R.string.gc_onboarding_page_1_message, DefaultOnboardingIllustrationAdapter(R.drawable.gc_onboarding_page_1)))
    class Page2 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_2_title, R.string.gc_onboarding_page_2_message, DefaultOnboardingIllustrationAdapter(R.drawable.gc_onboarding_page_2)))
    class Page3 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_3_title, R.string.gc_onboarding_page_3_message, DefaultOnboardingIllustrationAdapter(R.drawable.gc_onboarding_page_3)))
    class Page4 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_4_title, R.string.gc_onboarding_page_4_message, DefaultOnboardingIllustrationAdapter(R.drawable.gc_onboarding_page_4)))

    companion object {

        @JvmStatic
        fun asArrayList(isMultiPageEnabled: Boolean, isQRCodeScanningEnabled: Boolean): ArrayList<OnboardingPage> {
            val list = mutableListOf(
                Page1().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingAlignCornersIllustrationAdapter != null) {
                        onboardingPage.illustrationAdapter = GiniCapture.getInstance().onboardingAlignCornersIllustrationAdapter
                    }
                }.onboardingPage,
                Page2().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingLightingIllustrationAdapter != null) {
                        onboardingPage.illustrationAdapter = GiniCapture.getInstance().onboardingLightingIllustrationAdapter
                    }
                }.onboardingPage
            )
            if (isMultiPageEnabled) {
                list.add(
                    Page3().apply {
                        if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingMultiPageIllustrationAdapter != null) {
                            onboardingPage.illustrationAdapter = GiniCapture.getInstance().onboardingMultiPageIllustrationAdapter
                        }
                    }.onboardingPage
                )
            }
            if (isQRCodeScanningEnabled) {
                list.add(Page4().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingQRCodeIllustrationAdapter != null) {
                        onboardingPage.illustrationAdapter =
                            GiniCapture.getInstance().onboardingQRCodeIllustrationAdapter
                    }
                }.onboardingPage)
            }
            return ArrayList(list)
        }
    }
}
