package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.onboarding.view.DefaultOnboardingIconProvider

sealed class DefaultPages(val onboardingPage: OnboardingPage) {

    class Page1 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_1_title, R.string.gc_onboarding_page_1_message, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_page_1)))
    class Page2 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_2_title, R.string.gc_onboarding_page_2_message, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_page_2)))
    class Page3 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_3_title, R.string.gc_onboarding_page_3_message, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_page_3)))
    class Page4 : DefaultPages(OnboardingPage(R.string.gc_onboarding_page_4_title, R.string.gc_onboarding_page_4_message, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_page_4)))

    companion object {

        @JvmStatic
        fun asArrayList(isMultiPageEnabled: Boolean): ArrayList<OnboardingPage> {
            val list = mutableListOf(
                Page1().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingAlignCornersIconProvider != null) {
                        onboardingPage.iconProvider = GiniCapture.getInstance().onboardingAlignCornersIconProvider
                    }
                }.onboardingPage,
                Page2().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingLightingIconProvider != null) {
                        onboardingPage.iconProvider = GiniCapture.getInstance().onboardingLightingIconProvider
                    }
                }.onboardingPage
            )
            if (isMultiPageEnabled) {
                list.add(
                    Page3().apply {
                        if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingMultiPageIconProvider != null) {
                            onboardingPage.iconProvider = GiniCapture.getInstance().onboardingMultiPageIconProvider
                        }
                    }.onboardingPage
                )
            }
            list.add(Page4().apply {
                if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingQRCodeIconProvider != null) {
                    onboardingPage.iconProvider = GiniCapture.getInstance().onboardingQRCodeIconProvider
                }
            }.onboardingPage)
            return ArrayList(list)
        }
    }
}
