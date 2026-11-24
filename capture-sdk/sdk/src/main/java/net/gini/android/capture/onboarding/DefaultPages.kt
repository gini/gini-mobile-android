package net.gini.android.capture.onboarding

import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.onboarding.view.ImageOnboardingIllustrationAdapter
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter

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
                R.string.gc_onboarding_align_corners_title
            )
        )
    )

    class Page2 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_lighting_title,
            R.string.gc_onboarding_lighting_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_lighting,
                R.string.gc_onboarding_lighting_title
            )
        )
    )

    class Page3 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_multipage_title,
            R.string.gc_onboarding_multipage_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_multipage,
                R.string.gc_onboarding_multipage_title
            )
        )
    )

    class Page4 : DefaultPages(
        OnboardingPage(
            R.string.gc_onboarding_qr_code_title,
            R.string.gc_onboarding_qr_code_message,
            ImageOnboardingIllustrationAdapter(
                R.drawable.gc_onboarding_qr_code,
                R.string.gc_onboarding_qr_code_title)
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
            val giniCapture = if (GiniCapture.hasInstance()) GiniCapture.getInstance() else null
            val list = mutableListOf(
                Page1().onboardingPage
                    .withCustomIllustration(giniCapture?.onboardingAlignCornersIllustrationAdapter),
                Page2().onboardingPage
                    .withCustomIllustration(giniCapture?.onboardingLightingIllustrationAdapter)
            )
            if (isMultiPageEnabled) {
                list.add(
                    Page3().onboardingPage
                        .withCustomIllustration(giniCapture?.onboardingMultiPageIllustrationAdapter)
                )
            }
            if (isQRCodeScanningEnabled) {
                list.add(
                    Page4().onboardingPage
                        .withCustomIllustration(giniCapture?.onboardingQRCodeIllustrationAdapter)
                )
            }
            return ArrayList(list)
        }

        /**
         * Replaces the default illustration with the custom one, if the client configured any.
         */
        private fun OnboardingPage.withCustomIllustration(
            customAdapter: OnboardingIllustrationAdapter?
        ): OnboardingPage = apply {
            if (customAdapter != null) {
                illustrationAdapter = customAdapter
            }
        }
    }
}
