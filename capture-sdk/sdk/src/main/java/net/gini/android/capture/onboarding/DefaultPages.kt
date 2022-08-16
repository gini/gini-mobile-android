package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.ImageOnboardingIllustrationAdapter

/**
 * Default onboarding pages.
 */
sealed class DefaultPages(val onboardingPage: OnboardingPage) {

    class Page1 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_align_corners_title,
            R.string.gc_onboarding_align_corners_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_align_corners,
                R.string.gc_onboarding_align_corners_illustration_content_description
            )
        )
    )

    class Page2 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_lighting_title,
            R.string.gc_onboarding_lighting_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_lighting,
                R.string.gc_onboarding_lighting_title_illustration_content_description
            )
        )
    )

    class Page3 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_multipage_title,
            R.string.gc_onboarding_multipage_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_multipage,
                R.string.gc_onboarding_multipage_illustration_content_description
            )
        )
    )

    class Page4 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_qr_code_title,
            R.string.gc_onboarding_qr_code_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_qr_code,
                R.string.gc_onboarding_qr_code_illustration_content_description)
        )
    )

    companion object {

        /**
         * Get the default onboarding pages based on the enabled features.
         *
         * @param isMultiPageEnabled pass in `true` if the multi-page feature was enabled
         * @param isQRCodeScanningEnabled pass in `true` if the QR code scanning feature was enabled
         * @return an [ArrayList] of the onboarding pages
         */
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
