package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.internal.util.FeatureConfiguration
import net.gini.android.capture.onboarding.view.DefaultOnboardingIconProvider

sealed class DefaultPages(val onboardingPage: OnboardingPage) {

    class Align : DefaultPages(OnboardingPage(R.string.gc_onboarding_align, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_align)))
    class Lighting : DefaultPages(OnboardingPage(R.string.gc_onboarding_lighting, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_lighting)))
    class MultiPage : DefaultPages(OnboardingPage(R.string.gc_onboarding_multi_page, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_multipage)))
    class QRCode : DefaultPages(OnboardingPage(R.string.gc_onboarding_multi_page, DefaultOnboardingIconProvider(R.drawable.gc_onboarding_multipage)))

    companion object {

        @JvmStatic
        fun asArrayList(): ArrayList<OnboardingPage> {
            val list = mutableListOf(
                Align().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingAlignCornersIconProvider != null) {
                        onboardingPage.iconProvider = GiniCapture.getInstance().onboardingAlignCornersIconProvider
                    }
                }.onboardingPage,
                Lighting().apply {
                    if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingLightingIconProvider != null) {
                        onboardingPage.iconProvider = GiniCapture.getInstance().onboardingLightingIconProvider
                    }
                }.onboardingPage
            )
            if (FeatureConfiguration.isMultiPageEnabled()) {
                list.add(
                    MultiPage().apply {
                        if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingMultiPageIconProvider != null) {
                            onboardingPage.iconProvider = GiniCapture.getInstance().onboardingMultiPageIconProvider
                        }
                    }.onboardingPage
                )
            }
            list.add(QRCode().apply {
                if (GiniCapture.hasInstance() && GiniCapture.getInstance().onboardingQRCodeIconProvider != null) {
                    onboardingPage.iconProvider = GiniCapture.getInstance().onboardingQRCodeIconProvider
                }
            }.onboardingPage)
            return ArrayList(list)
        }
    }
}
